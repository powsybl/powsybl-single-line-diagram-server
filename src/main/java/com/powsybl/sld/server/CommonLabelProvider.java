package com.powsybl.sld.server;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.OperatingStatus;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.model.coordinate.Direction;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.model.nodes.*;
import com.powsybl.sld.svg.BusLegendInfo;
import com.powsybl.sld.svg.DefaultLabelProvider;
import com.powsybl.sld.svg.LabelProviderFactory;
import com.powsybl.sld.svg.SvgParameters;

import java.util.*;
import java.util.stream.DoubleStream;

public class CommonLabelProvider extends DefaultLabelProvider {
    private static final String UNIT_MW = "MW";
    private static final String UNIT_KV = "kV";
    private static final String UNIT_KA = "kA";

    private static final String PREFIX_VOLTAGE = "U = ";
    private static final String PREFIX_ANGLE = "θ = ";
    private static final String PREFIX_PRODUCTION = "P = ";
    private static final String PREFIX_CONSUMPTION = "C = ";
    private static final String PREFIX_ICC = "ICC = ";

    private static final String KEY_BUS_ID = "busId";
    public static final String KEY_VOLTAGE = "v";
    public static final String KEY_ANGLE = "angle";
    public static final String KEY_CONSUMPTION = "consumption-sum";
    public static final String KEY_PRODUCTION = "production-sum";
    public static final String KEY_ICC = "icc";

    private static final String PLANNED_OUTAGE_BRANCH_NODE_DECORATOR = "LOCK";
    private static final String FORCED_OUTAGE_BRANCH_NODE_DECORATOR = "FLASH";

    private final Map<String, Double> busIdToIccMap;

    public CommonLabelProvider(Network network, SldComponentLibrary componentLibrary, LayoutParameters layoutParameters, SvgParameters svgParameters) {
        super(network, componentLibrary, layoutParameters, svgParameters);
        this.busIdToIccMap = null;
    }

    public CommonLabelProvider(Network network, SldComponentLibrary componentLibrary, LayoutParameters layoutParameters, SvgParameters svgParameters, Map<String, Double> busIdToIccMap) {
        super(network, componentLibrary, layoutParameters, svgParameters);
        this.busIdToIccMap = busIdToIccMap;
    }

    public static LabelProviderFactory newCommonLabelProviderFactory(Map<String, Double> busIdToIccMap) {
        return (network, compLibrary, layoutParameters, svgParameters) -> new CommonLabelProvider(network, compLibrary, layoutParameters, svgParameters, busIdToIccMap);
    }

    @Override
    public List<BusLegendInfo> getBusLegendInfos(VoltageLevelGraph graph) {
        VoltageLevel vl = network.getVoltageLevel(graph.getVoltageLevelInfos().getId());
        return vl.getBusView().getBusStream()
            .map(b ->
                new BusLegendInfo(b.getId(), List.of(
                    new BusLegendInfo.Caption(b.getId(), KEY_BUS_ID),
                    new BusLegendInfo.Caption(PREFIX_VOLTAGE + valueFormatter.formatVoltage(b.getV(), UNIT_KV), KEY_VOLTAGE),
                    new BusLegendInfo.Caption(PREFIX_ANGLE + valueFormatter.formatAngleInDegrees(b.getAngle()), KEY_ANGLE),
                    new BusLegendInfo.Caption(PREFIX_PRODUCTION + formatPowerSum(b.getGeneratorStream().mapToDouble(g -> g.getTerminal().getP())), KEY_CONSUMPTION),
                    new BusLegendInfo.Caption(PREFIX_CONSUMPTION + formatPowerSum(b.getLoadStream().mapToDouble(l -> l.getTerminal().getP())), KEY_PRODUCTION),
                    new BusLegendInfo.Caption(PREFIX_ICC + getAndFormatBusIcc(b.getId()), KEY_ICC)
                ))
            ).toList();
    }

    @Override
    public List<NodeDecorator> getNodeDecorators(Node node, Direction direction) {
        Objects.requireNonNull(node);
        List<NodeDecorator> nodeDecorators = new ArrayList<>();
        if (node instanceof EquipmentNode equipmentNode && !(node instanceof SwitchNode)) {
            addDecoratorForEquipmentNode(nodeDecorators, node, equipmentNode, direction);
        }
        return nodeDecorators;
    }

    private void addDecoratorForEquipmentNode(List<NodeDecorator> nodeDecorators, Node node, EquipmentNode equipmentNode, Direction direction) {
        switch (equipmentNode) {
            case FeederNode feederNode -> addDecoratorForFeederNode(nodeDecorators, node, feederNode, direction);
            case Middle3WTNode middle3WTNode -> {
                if (middle3WTNode.isEmbeddedInVlGraph()) {
                    addDecoratorFor3WT(nodeDecorators, middle3WTNode, direction);
                }
            }
            default -> addDecoratorForGenericEquipment(nodeDecorators, equipmentNode, direction);
        }
    }

    private void addDecoratorForFeederNode(List<NodeDecorator> nodeDecorators, Node node, FeederNode feederNode, Direction direction) {
        switch (feederNode.getFeeder().getFeederType()) {
            case BRANCH, TWO_WINDINGS_TRANSFORMER_LEG ->
                    addDecoratorForBranch(nodeDecorators, feederNode, direction);
            case THREE_WINDINGS_TRANSFORMER_LEG ->
                    addDecoratorForThreeWindingsTransformer(nodeDecorators, node, feederNode, direction);
            case HVDC ->
                    addDecoratorForHvdc(nodeDecorators, feederNode, direction);
            default -> { /* No decorator for other feeder types */ }
        }
    }

    private void addDecoratorForBranch(List<NodeDecorator> nodeDecorators, FeederNode feederNode, Direction direction) {
        Branch<?> branch = network.getBranch(feederNode.getEquipmentId());
        if (!branch.getTerminal1().isConnected() && !branch.getTerminal2().isConnected()) {
            getOperatingStatusDecorator(nodeDecorators, feederNode, direction, branch);
        }
    }

    private void addDecoratorForThreeWindingsTransformer(List<NodeDecorator> nodeDecorators, Node node, FeederNode feederNode, Direction direction) {
        if (node.getAdjacentNodes().stream().noneMatch(Middle3WTNode.class::isInstance)) {
            ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(feederNode.getEquipmentId());
            if (!twt.getLeg1().getTerminal().isConnected() && !twt.getLeg2().getTerminal().isConnected() && !twt.getLeg3().getTerminal().isConnected()) {
                getOperatingStatusDecorator(nodeDecorators, feederNode, direction, twt);
            }
        }
    }

    private void addDecoratorForHvdc(List<NodeDecorator> nodeDecorators, FeederNode feederNode, Direction direction) {
        HvdcLine hvdcLine = network.getHvdcLine(feederNode.getEquipmentId());
        if (!hvdcLine.getConverterStation1().getTerminal().isConnected() && !hvdcLine.getConverterStation2().getTerminal().isConnected()) {
            getOperatingStatusDecorator(nodeDecorators, feederNode, direction, hvdcLine);
        }
    }

    private void addDecoratorFor3WT(List<NodeDecorator> nodeDecorators, Middle3WTNode middle3WTNode, Direction direction) {
        ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(middle3WTNode.getEquipmentId());
        if (!twt.getLeg1().getTerminal().isConnected() && !twt.getLeg2().getTerminal().isConnected() && !twt.getLeg3().getTerminal().isConnected()) {
            getOperatingStatusDecorator(nodeDecorators, middle3WTNode, direction, twt);
        }
    }

    private void addDecoratorForGenericEquipment(List<NodeDecorator> nodeDecorators, EquipmentNode equipmentNode, Direction direction) {
        Identifiable<?> identifiable = network.getIdentifiable(equipmentNode.getEquipmentId());
        if (identifiable instanceof Connectable<?> connectable && !connectable.getTerminals().stream().allMatch(Terminal::isConnected)) {
            getOperatingStatusDecorator(nodeDecorators, equipmentNode, direction, identifiable);
        }
    }

    private <T extends Identifiable<T>> void getOperatingStatusDecorator(List<NodeDecorator> nodeDecorators, Node node, Direction direction, Identifiable<T> identifiable) {
        if (identifiable != null) {
            OperatingStatus<T> operatingStatus = identifiable.getExtension(OperatingStatus.class);
            if (operatingStatus != null) {
                switch (operatingStatus.getStatus()) {
                    case PLANNED_OUTAGE -> nodeDecorators.add(addOperatingStatusDecorator(node, direction, PLANNED_OUTAGE_BRANCH_NODE_DECORATOR));
                    case FORCED_OUTAGE -> nodeDecorators.add(addOperatingStatusDecorator(node, direction, FORCED_OUTAGE_BRANCH_NODE_DECORATOR));
                    case IN_OPERATION -> { /* No decorator for IN_OPERATION equipment */ }
                }
            }
        }
    }

    private NodeDecorator addOperatingStatusDecorator(Node node, Direction direction, String decoratorType) {
        return switch (node) {
            case Middle3WTNode middle3WTNode -> new NodeDecorator(decoratorType, getMiddle3WTDecoratorPosition(middle3WTNode, direction));
            case BusNode ignored2 -> new NodeDecorator(decoratorType, getBusDecoratorPosition());
            case FeederNode ignored1 -> new NodeDecorator(decoratorType, getFeederDecoratorPosition(direction, decoratorType));
            case Internal2WTNode ignored -> new NodeDecorator(decoratorType, getInternal2WTDecoratorPosition(node.getOrientation()));
            case null, default -> new NodeDecorator(decoratorType, getGenericDecoratorPosition());
        };
    }

    private String getAndFormatBusIcc(String busId) {
        Double iccInA = busIdToIccMap == null ? null : busIdToIccMap.get(busId);

        String value = iccInA != null
            ? String.format("%.1f", iccInA / 1000.0)
            : svgParameters.getUndefinedValueSymbol();

        return value + " " + UNIT_KA;
    }

    private String formatPowerSum(DoubleStream stream) {
        OptionalDouble sum = sumDoubleStream(stream);
        String value = sum.isPresent()
            ? String.valueOf(Math.round(Math.abs(sum.getAsDouble())))
            : svgParameters.getUndefinedValueSymbol();
        return value + " " + UNIT_MW;
    }

    private static OptionalDouble sumDoubleStream(DoubleStream stream) {
        DoubleSummaryStatistics stats = stream.summaryStatistics();
        return stats.getCount() == 0
            ? OptionalDouble.empty()
            : OptionalDouble.of(stats.getSum());
    }
}
