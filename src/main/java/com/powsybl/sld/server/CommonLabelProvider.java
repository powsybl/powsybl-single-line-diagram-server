package com.powsybl.sld.server;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.OperatingStatus;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.model.coordinate.Direction;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.model.nodes.*;
import com.powsybl.sld.model.nodes.feeders.FeederWithSides;
import com.powsybl.sld.svg.BusLegendInfo;
import com.powsybl.sld.svg.DefaultLabelProvider;
import com.powsybl.sld.svg.SvgParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CommonLabelProvider extends DefaultLabelProvider {

    private static final String PLANNED_OUTAGE_BRANCH_NODE_DECORATOR = "LOCK";
    private static final String FORCED_OUTAGE_BRANCH_NODE_DECORATOR = "FLASH";

    public CommonLabelProvider(Network network, SldComponentLibrary componentLibrary, LayoutParameters layoutParameters, SvgParameters svgParameters) {
        super(network, componentLibrary, layoutParameters, svgParameters);
    }

    @Override
    public List<BusLegendInfo> getBusLegendInfos(VoltageLevelGraph graph) {
        VoltageLevel vl = network.getVoltageLevel(graph.getVoltageLevelInfos().getId());
        return vl.getBusView().getBusStream()
            .map(b -> new BusLegendInfo(b.getId(), List.of(
                new BusLegendInfo.Caption(b.getId(), "busId"),
                new BusLegendInfo.Caption(valueFormatter.formatVoltage(b.getV(), "kV"), "v"),
                new BusLegendInfo.Caption(valueFormatter.formatAngleInDegrees(b.getAngle()), "angle")
            )))
            .collect(Collectors.toList());
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
        if (displayStatusDecorator(branch, feederNode.getFeeder())) {
            getOperatingStatusDecorator(nodeDecorators, feederNode, direction, branch);
        }
    }

    private void addDecoratorForThreeWindingsTransformer(List<NodeDecorator> nodeDecorators, Node node, FeederNode feederNode, Direction direction) {
        if (node.getAdjacentNodes().stream().noneMatch(Middle3WTNode.class::isInstance)) {
            ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(feederNode.getEquipmentId());
            if (displayStatusDecorator(twt, feederNode.getFeeder())) {
                getOperatingStatusDecorator(nodeDecorators, feederNode, direction, twt);
            }
        }
    }

    private void addDecoratorForHvdc(List<NodeDecorator> nodeDecorators, FeederNode feederNode, Direction direction) {
        HvdcLine hvdcLine = network.getHvdcLine(feederNode.getEquipmentId());
        if (displayStatusDecorator(hvdcLine, feederNode.getFeeder())) {
            getOperatingStatusDecorator(nodeDecorators, feederNode, direction, hvdcLine);
        }
    }

    private void addDecoratorFor3WT(List<NodeDecorator> nodeDecorators, Middle3WTNode middle3WTNode, Direction direction) {
        ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(middle3WTNode.getEquipmentId());
        if (displayStatusDecorator(twt)) {
            getOperatingStatusDecorator(nodeDecorators, middle3WTNode, direction, twt);
        }
    }

    private void addDecoratorForGenericEquipment(List<NodeDecorator> nodeDecorators, EquipmentNode equipmentNode, Direction direction) {
        Identifiable<?> identifiable = network.getIdentifiable(equipmentNode.getEquipmentId());
        if (displayStatusDecorator(identifiable)) {
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
            case Middle3WTNode middle3WTNode ->
                    new NodeDecorator(decoratorType, getMiddle3WTDecoratorPosition(middle3WTNode, direction));
            case BusNode ignored2 -> new NodeDecorator(decoratorType, getBusDecoratorPosition());
            case FeederNode ignored1 ->
                    new NodeDecorator(decoratorType, getFeederDecoratorPosition(direction, decoratorType));
            case Internal2WTNode ignored ->
                    new NodeDecorator(decoratorType, getInternal2WTDecoratorPosition(node.getOrientation()));
            case null, default -> new NodeDecorator(decoratorType, getGenericDecoratorPosition());
        };
    }

    private boolean displayStatusDecorator(Identifiable<?> identifiable, Feeder feeder) {
        if (!(feeder instanceof FeederWithSides feederWithSides)) {
            return false;
        }
        NodeSide side = feederWithSides.getSide();
        if (identifiable instanceof Branch<?> branch) {
            Terminal terminal = side == NodeSide.ONE ? branch.getTerminal1() : branch.getTerminal2();
            return !terminal.isConnected();
        } else if (identifiable instanceof ThreeWindingsTransformer twt) {
            Terminal terminal = switch (side) {
                case ONE -> twt.getLeg1().getTerminal();
                case TWO -> twt.getLeg2().getTerminal();
                case THREE -> twt.getLeg3().getTerminal();
            };
            return !terminal.isConnected();
        } else if (identifiable instanceof HvdcLine hvdcLine) {
            Terminal terminal = side == NodeSide.ONE ? hvdcLine.getConverterStation1().getTerminal() : hvdcLine.getConverterStation2().getTerminal();
            return !terminal.isConnected();
        }
        return false;
    }

    private boolean displayStatusDecorator(Identifiable<?> identifiable) {
        if (identifiable instanceof Connectable<?> connectable) {
            return connectable.getTerminals().stream().noneMatch(Terminal::isConnected);
        }
        return false;
    }
}
