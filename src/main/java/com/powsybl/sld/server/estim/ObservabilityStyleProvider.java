/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.sld.server.estim;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.TwoWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.ObservabilityArea;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.model.graphs.Graph;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.model.graphs.VoltageLevelInfos;
import com.powsybl.sld.model.nodes.BusNode;
import com.powsybl.sld.model.nodes.Edge;
import com.powsybl.sld.model.nodes.EquipmentNode;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.model.nodes.GroundDisconnectionNode;
import com.powsybl.sld.model.nodes.Middle2WTNode;
import com.powsybl.sld.model.nodes.Middle3WTNode;
import com.powsybl.sld.model.nodes.Node;
import com.powsybl.sld.model.nodes.SwitchNode;
import com.powsybl.sld.model.nodes.feeders.FeederTwLeg;
import com.powsybl.sld.svg.styles.EmptyStyleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
public class ObservabilityStyleProvider extends EmptyStyleProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ObservabilityStyleProvider.class);

    private static final String NO_INFORMATION_CSS = "sld-observability-no-information";
    private static final String OBSERVABLE_CSS = "sld-observability-observable";
    private static final String NON_OBSERVABLE_CSS = "sld-observability-non-observable";
    private static final String BORDER_CSS = "sld-observability-border";
    private static final List<String> NO_INFORMATION_STYLES = List.of(NO_INFORMATION_CSS);
    private static final String WINDING_1 = "WINDING1";
    private static final String WINDING_2 = "WINDING2";
    private static final String WINDING_3 = "WINDING3";
    private static final Set<String> EQUIPMENT_SUBCOMPONENTS = Set.of(
            "LOAD",
            "BATTERY",
            "GENERATOR",
            "CAPACITOR",
            "INDUCTOR",
            "STATIC_VAR_COMPENSATOR",
            "VSC_CONVERTER_STATION",
            "LCC_CONVERTER_STATION",
            "BUS_CONNECTION",
            "NODE",
            "GROUND");

    private final Network network;
    private final Map<BusResolutionKey, Optional<Bus>> busCache = new HashMap<>();
    private final Map<NodeBusResolutionKey, Optional<Bus>> nodeBusCache = new HashMap<>();
    private final Map<WindingResolutionKey, Optional<Bus>> windingBusCache = new HashMap<>();
    private final Map<Terminal, Optional<Bus>> terminalBusCache = new HashMap<>();
    private final Map<String, List<String>> observabilityStylesCache = new HashMap<>();

    public ObservabilityStyleProvider(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    @Override
    public List<String> getEdgeStyles(Graph graph, Edge edge) {
        return resolveEdgeBus(graph, edge).map(this::getObservabilityStyles).orElse(NO_INFORMATION_STYLES);
    }

    @Override
    public List<String> getNodeStyles(VoltageLevelGraph graph, Node node, SldComponentLibrary componentLibrary, boolean showInternalNodes) {
        if (node instanceof SwitchNode) {
            return List.of();
        }
        return resolveBus(graph, node).map(this::getObservabilityStyles).orElse(NO_INFORMATION_STYLES);
    }

    @Override
    public List<String> getNodeSubcomponentStyles(Graph graph, Node node, String subComponentName) {
        if (isWindingSubcomponent(subComponentName)) {
            if (!isSupportedWinding(node, subComponentName)) {
                return List.of();
            }
            return resolveWindingBus(graph, node, subComponentName)
                    .map(this::getObservabilityStyles)
                    .orElse(NO_INFORMATION_STYLES);
        }

        if (!isEquipmentSubcomponent(node, subComponentName)) {
            return List.of();
        }

        return resolveBus(graph, node).map(this::getObservabilityStyles).orElse(NO_INFORMATION_STYLES);
    }

    private boolean isEquipmentSubcomponent(Node node, String subComponentName) {
        return EQUIPMENT_SUBCOMPONENTS.contains(subComponentName)
                && !(node instanceof SwitchNode)
                && !(node instanceof GroundDisconnectionNode);
    }

    private boolean isWindingSubcomponent(String subComponentName) {
        return WINDING_1.equals(subComponentName)
                || WINDING_2.equals(subComponentName)
                || WINDING_3.equals(subComponentName);
    }

    private boolean isSupportedWinding(Node node, String subComponentName) {
        return node instanceof FeederNode
                || node instanceof Middle3WTNode
                || node instanceof Middle2WTNode && !WINDING_3.equals(subComponentName);
    }

    private Optional<Bus> resolveWindingBus(Graph graph, Node node, String subComponentName) {
        WindingResolutionKey key = new WindingResolutionKey(graph, node, subComponentName);
        return windingBusCache.computeIfAbsent(key, ignored -> resolveWindingBusUncached(graph, node, subComponentName));
    }

    private Optional<Bus> resolveWindingBusUncached(Graph graph, Node node, String subComponentName) {
        if (!(node instanceof FeederNode) && !(node instanceof Middle2WTNode) && !(node instanceof Middle3WTNode)) {
            return Optional.empty();
        }

        String equipmentId = ((EquipmentNode) node).getEquipmentId();
        if (node instanceof Middle3WTNode middle3WTNode) {
            ThreeWindingsTransformer transformer = network.getThreeWindingsTransformer(equipmentId);
            return transformer == null
                    ? Optional.empty()
                    : resolveThreeWindingWindingBus(middle3WTNode, transformer, subComponentName);
        }

        TwoWindingsTransformer twoWindingsTransformer = network.getTwoWindingsTransformer(equipmentId);
        if (twoWindingsTransformer != null) {
            if (node instanceof FeederNode && graph != null) {
                return resolveVoltageLevelWindingBus(graph, node, twoWindingsTransformer, subComponentName);
            }
            return resolveTerminalBus(WINDING_1.equals(subComponentName)
                    ? twoWindingsTransformer.getTerminal1()
                    : twoWindingsTransformer.getTerminal2());
        }

        ThreeWindingsTransformer transformer = network.getThreeWindingsTransformer(equipmentId);
        if (transformer == null) {
            return Optional.empty();
        }
        return switch (subComponentName) {
            case WINDING_1 -> resolveTerminalBus(transformer.getLeg1().getTerminal());
            case WINDING_2 -> resolveTerminalBus(transformer.getLeg2().getTerminal());
            case WINDING_3 -> resolveTerminalBus(transformer.getLeg3().getTerminal());
            default -> Optional.empty();
        };
    }

    private Optional<Bus> resolveThreeWindingWindingBus(Middle3WTNode node,
                                                        ThreeWindingsTransformer transformer,
                                                        String subComponentName) {
        Middle3WTNode.Winding winding = switch (subComponentName) {
            case WINDING_1 -> Middle3WTNode.Winding.UPPER_LEFT;
            case WINDING_2 -> Middle3WTNode.Winding.UPPER_RIGHT;
            case WINDING_3 -> Middle3WTNode.Winding.DOWN;
            default -> null;
        };
        if (winding == null) {
            return Optional.empty();
        }

        VoltageLevelInfos voltageLevelInfos = node.getVoltageLevelInfos(winding);
        if (voltageLevelInfos != null) {
            String voltageLevelId = voltageLevelInfos.id();
            if (voltageLevelId == null) {
                return Optional.empty();
            }
            return resolveUniqueTerminalBus(getThreeWindingTerminals(transformer), voltageLevelId);
        }

        return resolveTerminalBus(switch (subComponentName) {
            case WINDING_1 -> transformer.getLeg1().getTerminal();
            case WINDING_2 -> transformer.getLeg2().getTerminal();
            case WINDING_3 -> transformer.getLeg3().getTerminal();
            default -> null;
        });
    }

    private List<Terminal> getThreeWindingTerminals(ThreeWindingsTransformer transformer) {
        List<Terminal> terminals = new ArrayList<>(3);
        if (transformer.getLeg1().getTerminal() != null) {
            terminals.add(transformer.getLeg1().getTerminal());
        }
        if (transformer.getLeg2().getTerminal() != null) {
            terminals.add(transformer.getLeg2().getTerminal());
        }
        if (transformer.getLeg3().getTerminal() != null) {
            terminals.add(transformer.getLeg3().getTerminal());
        }
        return terminals;
    }

    private Optional<Bus> resolveVoltageLevelWindingBus(Graph graph, Node node,
                                                        TwoWindingsTransformer transformer, String subComponentName) {
        VoltageLevelGraph voltageLevelGraph = graph.getVoltageLevelGraph(node);
        if (voltageLevelGraph == null || voltageLevelGraph.getVoltageLevelInfos() == null) {
            return Optional.empty();
        }

        VoltageLevelInfos voltageLevelInfos = voltageLevelGraph.getVoltageLevelInfos();
        if (voltageLevelInfos == null || voltageLevelInfos.id() == null) {
            return Optional.empty();
        }

        String voltageLevelId = voltageLevelInfos.id();
        Terminal terminal1 = transformer.getTerminal1();
        Terminal terminal2 = transformer.getTerminal2();
        List<Terminal> transformerTerminals = new ArrayList<>(2);
        if (terminal1 != null) {
            transformerTerminals.add(terminal1);
        }
        if (terminal2 != null) {
            transformerTerminals.add(terminal2);
        }
        List<Terminal> localTerminals = getTerminalsForVoltageLevel(transformerTerminals, voltageLevelId);
        if (localTerminals.size() != 1) {
            return Optional.empty();
        }

        Terminal localTerminal = localTerminals.getFirst();
        Terminal outboundTerminal = localTerminal == terminal1 ? terminal2 : terminal1;
        Terminal windingTerminal = WINDING_1.equals(subComponentName) ? localTerminal : outboundTerminal;
        return resolveTerminalBus(windingTerminal);
    }

    private Optional<Bus> resolveUniqueTerminalBus(List<? extends Terminal> terminals, String voltageLevelId) {
        List<Terminal> matchingTerminals = getTerminalsForVoltageLevel(terminals, voltageLevelId);
        return matchingTerminals.size() == 1 ? resolveTerminalBus(matchingTerminals.getFirst()) : Optional.empty();
    }

    private List<Terminal> getTerminalsForVoltageLevel(List<? extends Terminal> terminals, String voltageLevelId) {
        List<Terminal> matchingTerminals = new ArrayList<>();
        for (Terminal terminal : terminals) {
            if (terminal != null && terminal.getVoltageLevel() != null
                    && voltageLevelId.equals(terminal.getVoltageLevel().getId())) {
                matchingTerminals.add(terminal);
            }
        }
        return matchingTerminals;
    }

    private Optional<Bus> resolveTerminalBus(Terminal terminal) {
        if (terminal == null) {
            return Optional.empty();
        }
        return terminalBusCache.computeIfAbsent(terminal, ignored -> Optional.ofNullable(terminal.getBusView())
                .map(Terminal.BusView::getBus));
    }

    private Optional<Bus> resolveEdgeBus(Graph graph, Edge edge) {
        if (graph == null) {
            return Optional.empty();
        }

        Optional<Node> threeWindingFeeder = edge.getNodes().stream()
                .filter(FeederNode.class::isInstance)
                .filter(ignored -> edge.getNodes().stream().anyMatch(Middle3WTNode.class::isInstance))
                .findFirst();
        if (threeWindingFeeder.isPresent()) {
            return resolveThreeWindingEdgeBus((FeederNode) threeWindingFeeder.get());
        }

        for (Node node : edge.getNodes()) {
            VoltageLevelGraph voltageLevelGraph = graph.getVoltageLevelGraph(node);
            if (voltageLevelGraph != null) {
                return resolveBus(graph, node, voltageLevelGraph);
            }
        }
        return Optional.empty();
    }

    private Optional<Bus> resolveThreeWindingEdgeBus(FeederNode feederNode) {
        if (!(feederNode.getFeeder() instanceof FeederTwLeg feeder)
                || feeder.getOtherSideVoltageLevelInfos() == null) {
            return Optional.empty();
        }

        ThreeWindingsTransformer transformer = network.getThreeWindingsTransformer(feederNode.getEquipmentId());
        if (transformer == null) {
            return Optional.empty();
        }

        Terminal terminal = switch (feeder.getSide()) {
            case ONE -> transformer.getLeg1().getTerminal();
            case TWO -> transformer.getLeg2().getTerminal();
            case THREE -> transformer.getLeg3().getTerminal();
        };
        return resolveTerminalBus(terminal);
    }

    private Optional<Bus> resolveBus(Graph graph, Node node) {
        return resolveBus(graph, node, graph == null ? null : graph.getVoltageLevelGraph(node));
    }

    private Optional<Bus> resolveBus(Graph graph, Node node, VoltageLevelGraph voltageLevelGraph) {
        BusResolutionKey key = new BusResolutionKey(graph, node, voltageLevelGraph);
        return busCache.computeIfAbsent(key, ignored -> resolveBusUncached(graph, node, voltageLevelGraph));
    }

    private Optional<Bus> resolveBusUncached(Graph graph, Node node, VoltageLevelGraph voltageLevelGraph) {
        if (node instanceof BusNode busNode) {
            return resolveNodeBus(busNode, voltageLevelGraph);
        }

        if (node instanceof EquipmentNode equipmentNode) {
            if (Node.NodeType.INTERNAL.equals(equipmentNode.getType())) {
                return Optional.empty();
            }
            Optional<Bus> equipmentBus = resolveNodeBus(equipmentNode, voltageLevelGraph);
            if (equipmentBus.isPresent() || !(equipmentNode instanceof SwitchNode)) {
                return equipmentBus;
            }
            return resolveConnectedBus(graph, node, voltageLevelGraph);
        }

        return resolveConnectedBus(graph, node);
    }

    private Optional<Bus> resolveConnectedBus(Graph graph, Node node) {
        return resolveConnectedBus(graph, node, null);
    }

    private Optional<Bus> resolveConnectedBus(Graph graph, Node node, VoltageLevelGraph preferredVoltageLevelGraph) {
        Set<Node> connectedNodes = new LinkedHashSet<>();
        collectConnectedNodes(node, connectedNodes);

        Map<String, Bus> buses = new LinkedHashMap<>();
        for (Node connectedNode : connectedNodes) {
            Optional<Bus> bus = connectedNode == node
                    ? resolveNodeBus(connectedNode, preferredVoltageLevelGraph)
                    : resolveNodeBus(graph, connectedNode);
            bus.ifPresent(value -> buses.putIfAbsent(value.getId(), value));
        }
        return buses.size() == 1 ? Optional.of(buses.values().iterator().next()) : Optional.empty();
    }

    private Optional<Bus> resolveNodeBus(Graph graph, Node node) {
        VoltageLevelGraph voltageLevelGraph = graph == null ? null : graph.getVoltageLevelGraph(node);
        return resolveNodeBus(node, voltageLevelGraph);
    }

    private Optional<Bus> resolveNodeBus(Node node, VoltageLevelGraph voltageLevelGraph) {
        NodeBusResolutionKey key = new NodeBusResolutionKey(node, voltageLevelGraph);
        return nodeBusCache.computeIfAbsent(key, ignored -> resolveNodeBusUncached(node, voltageLevelGraph));
    }

    private Optional<Bus> resolveNodeBusUncached(Node node, VoltageLevelGraph voltageLevelGraph) {
        if (node instanceof BusNode busNode) {
            return resolveBusNode(busNode);
        }
        if (node instanceof EquipmentNode equipmentNode) {
            return resolveEquipmentBus(voltageLevelGraph, equipmentNode);
        }
        return Optional.empty();
    }

    private Optional<Bus> resolveBusNode(BusNode busNode) {
        String id = busNode.getEquipmentId();
        BusbarSection busbarSection = network.getBusbarSection(id);
        if (busbarSection != null) {
            return Optional.ofNullable(busbarSection.getTerminal())
                    .map(Terminal::getBusView)
                    .map(Terminal.BusView::getBus);
        }
        return Optional.ofNullable(network.getBusView())
                .map(busView -> busView.getBus(id));
    }

    private Optional<Bus> resolveEquipmentBus(VoltageLevelGraph voltageLevelGraph, EquipmentNode equipmentNode) {
        if (voltageLevelGraph == null || voltageLevelGraph.getVoltageLevelInfos() == null) {
            return Optional.empty();
        }

        if (Node.NodeType.INTERNAL.equals(equipmentNode.getType())) {
            return Optional.empty();
        }

        Identifiable<?> identifiable = network.getIdentifiable(equipmentNode.getEquipmentId());
        if (!(identifiable instanceof Connectable<?> connectable)) {
            return Optional.empty();
        }

        String voltageLevelId = voltageLevelGraph.getVoltageLevelInfos().id();
        if (voltageLevelId == null) {
            return Optional.empty();
        }
        List<Terminal> terminals = connectable.getTerminals().stream()
                .filter(t -> t.getVoltageLevel() != null)
                .filter(t -> t.getVoltageLevel().getId().equals(voltageLevelId))
                .collect(Collectors.toList());

        if (terminals.size() != 1) {
            return Optional.empty();
        }

        return Optional.ofNullable(terminals.getFirst().getBusView())
                .map(Terminal.BusView::getBus);
    }

    private void collectConnectedNodes(Node node, Set<Node> visited) {
        if (!visited.add(node)) {
            return;
        }
        if (node instanceof SwitchNode switchNode && switchNode.isOpen()) {
            return;
        }
        node.getAdjacentNodes().forEach(n -> collectConnectedNodes(n, visited));
    }

    private List<String> getObservabilityStyles(Bus bus) {
        return observabilityStylesCache.computeIfAbsent(bus.getId(), ignored -> resolveObservabilityStyles(bus));
    }

    private List<String> resolveObservabilityStyles(Bus bus) {
        VoltageLevel busVoltageLevel = bus.getVoltageLevel();
        if (busVoltageLevel == null) {
            return NO_INFORMATION_STYLES;
        }

        ObservabilityArea extension = getObservabilityArea(busVoltageLevel);
        if (extension == null || extension.getBusView() == null || !extension.isConsistentWithTopology()) {
            return NO_INFORMATION_STYLES;
        }

        ObservabilityArea.AreaCharacteristics area;
        try {
            area = extension.getBusView().getObservabilityArea(bus.getId());
        } catch (PowsyblException e) {
            LOGGER.warn("Unable to get observability area for bus {}", bus.getId(), e);
            return NO_INFORMATION_STYLES;
        }
        if (area == null || area.getStatus() == null) {
            return NO_INFORMATION_STYLES;
        }

        return switch (area.getStatus()) {
            case OBSERVABLE -> List.of(OBSERVABLE_CSS);
            case NON_OBSERVABLE -> List.of(NON_OBSERVABLE_CSS);
            case BORDER -> List.of(BORDER_CSS);
        };
    }

    private ObservabilityArea getObservabilityArea(VoltageLevel voltageLevel) {
        try {
            return voltageLevel.getExtension(ObservabilityArea.class);
        } catch (PowsyblException e) {
            LOGGER.warn("Unable to get observability area for voltage level {}", voltageLevel.getId(), e);
            return null;
        }
    }

    private record BusResolutionKey(Graph graph, Node node, VoltageLevelGraph voltageLevelGraph) {
    }

    private record NodeBusResolutionKey(Node node, VoltageLevelGraph voltageLevelGraph) {
    }

    private record WindingResolutionKey(Graph graph, Node node, String subComponentName) {
    }
}
