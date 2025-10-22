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
            if (node instanceof FeederNode feederNode) {
                switch (feederNode.getFeeder().getFeederType()) {
                    case BRANCH, TWO_WINDINGS_TRANSFORMER_LEG -> {
                        Branch<?> branch = network.getBranch(feederNode.getEquipmentId());
                        if (shouldDisplayStatusDecorator(branch, feederNode.getFeeder())) {
                            addOperatingStatusDecorator(nodeDecorators, node, direction, branch);
                        }
                    }
                    case THREE_WINDINGS_TRANSFORMER_LEG -> {
                        if (node.getAdjacentNodes().stream().noneMatch(Middle3WTNode.class::isInstance)) {
                            ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(feederNode.getEquipmentId());
                            if (shouldDisplayStatusDecorator(twt, feederNode.getFeeder())) {
                                addOperatingStatusDecorator(nodeDecorators, node, direction, twt);
                            }
                        }
                    }
                    case HVDC -> {
                        HvdcLine hvdcLine = network.getHvdcLine(feederNode.getEquipmentId());
                        if (shouldDisplayStatusDecorator(hvdcLine, feederNode.getFeeder())) {
                            addOperatingStatusDecorator(nodeDecorators, node, direction, hvdcLine);
                        }
                    }
                    default -> { /* No decorator for other feeder types */ }
                }
            } else if (node instanceof MiddleTwtNode) {
                if (node instanceof Middle3WTNode middle3WTNode && middle3WTNode.isEmbeddedInVlGraph()) {
                    ThreeWindingsTransformer twt = network.getThreeWindingsTransformer(middle3WTNode.getEquipmentId());
                    if (shouldDisplayStatusDecorator(twt)) {
                        addOperatingStatusDecorator(nodeDecorators, node, direction, twt);
                    }
                }
            } else {
                Identifiable<?> identifiable = network.getIdentifiable(equipmentNode.getEquipmentId());
                if (shouldDisplayStatusDecorator(identifiable)) {
                    addOperatingStatusDecorator(nodeDecorators, node, direction, identifiable);
                }
            }
        }

        return nodeDecorators;
    }

    private <T extends Identifiable<T>> void addOperatingStatusDecorator(List<NodeDecorator> nodeDecorators, Node node, Direction direction, Identifiable<T> identifiable) {
        if (identifiable != null) {
            OperatingStatus<T> operatingStatus = identifiable.getExtension(OperatingStatus.class);
            if (operatingStatus != null) {
                switch (operatingStatus.getStatus()) {
                    case PLANNED_OUTAGE -> nodeDecorators.add(getOperatingStatusDecorator(node, direction, PLANNED_OUTAGE_BRANCH_NODE_DECORATOR));
                    case FORCED_OUTAGE -> nodeDecorators.add(getOperatingStatusDecorator(node, direction, FORCED_OUTAGE_BRANCH_NODE_DECORATOR));
                    case IN_OPERATION -> { /* No decorator for IN_OPERATION equipment */ }
                }
            }
        }
    }

    private NodeDecorator getOperatingStatusDecorator(Node node, Direction direction, String decoratorType) {
        if (node instanceof Middle3WTNode middle3WTNode) {
            return new NodeDecorator(decoratorType, getMiddle3WTDecoratorPosition(middle3WTNode, direction));
        } else if (node instanceof BusNode) {
            return new NodeDecorator(decoratorType, getBusDecoratorPosition());
        } else if (node instanceof FeederNode) {
            return new NodeDecorator(decoratorType, getFeederDecoratorPosition(direction, decoratorType));
        } else if (node instanceof Internal2WTNode) {
            return new NodeDecorator(decoratorType, getInternal2WTDecoratorPosition(node.getOrientation()));
        } else {
            return new NodeDecorator(decoratorType, getGenericDecoratorPosition());
        }
    }

    private boolean shouldDisplayStatusDecorator(Identifiable<?> identifiable, Feeder feeder) {
        if (!(feeder instanceof FeederWithSides feederWithSides)) {
            return true;
        }
        NodeSide side = feederWithSides.getSide();
        if (identifiable instanceof Branch<?> branch) {
            Terminal terminal = side == NodeSide.ONE ? branch.getTerminal1() : branch.getTerminal2();
            return terminal.isConnected();
        } else if (identifiable instanceof ThreeWindingsTransformer twt) {
            Terminal terminal = switch (side) {
                case ONE -> twt.getLeg1().getTerminal();
                case TWO -> twt.getLeg2().getTerminal();
                case THREE -> twt.getLeg3().getTerminal();
            };
            return terminal.isConnected();
        } else if (identifiable instanceof HvdcLine hvdcLine) {
            Terminal terminal = side == NodeSide.ONE ? hvdcLine.getConverterStation1().getTerminal() : hvdcLine.getConverterStation2().getTerminal();
            return terminal.isConnected();
        }
        return true;
    }

    private boolean shouldDisplayStatusDecorator(Identifiable<?> identifiable) {
        if (identifiable instanceof Connectable<?> connectable) {
            return connectable.getTerminals().stream().anyMatch(terminal -> !terminal.isConnected());
        }
        return true;
    }
}
