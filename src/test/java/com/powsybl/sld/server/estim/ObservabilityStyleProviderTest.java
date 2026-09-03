/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.sld.server.estim;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ObservabilityArea;
import com.powsybl.network.store.model.ExtensionLoaders;
import com.powsybl.sld.model.graphs.Graph;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.model.graphs.VoltageLevelInfos;
import com.powsybl.sld.model.nodes.*;
import com.powsybl.sld.model.nodes.feeders.FeederTwLeg;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ObservabilityStyleProviderTest {

    @Test
    void networkStoreProvidesObservabilityAreaLoader() {
        assertThat(ExtensionLoaders.loaderExists(ObservabilityArea.class)).isTrue();
    }

    @Test
    void edgeStyleUsesEndpointInDiagram() {
        Network network = mock(Network.class);
        Network.BusView busView = mock(Network.BusView.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        ObservabilityArea.AreaCharacteristics areaCharacteristics = mock(ObservabilityArea.AreaCharacteristics.class);
        Graph graph = mock(Graph.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        Node outsideEndpoint = mock(Node.class);
        BusNode localEndpoint = new BusNode("localBus", "localBus", false);

        when(graph.getVoltageLevelGraph(outsideEndpoint)).thenReturn(null);
        when(graph.getVoltageLevelGraph(localEndpoint)).thenReturn(voltageLevelGraph);
        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("localBus")).thenReturn(bus);
        when(bus.getId()).thenReturn("localBus");
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityArea.isConsistentWithTopology()).thenReturn(true);
        when(observabilityBusView.getObservabilityArea("localBus")).thenReturn(areaCharacteristics);
        when(areaCharacteristics.getStatus()).thenReturn(ObservabilityArea.ObservabilityStatus.OBSERVABLE);

        Edge edge = new Edge(outsideEndpoint, localEndpoint);

        assertThat(new ObservabilityStyleProvider(network).getEdgeStyles(graph, edge))
                .containsExactly("sld-observability-observable");
    }

    @Test
    void edgeStyleUsesBusInDiagramForMultiTerminalEquipment() {
        Network network = mock(Network.class);
        Network.BusView busView = mock(Network.BusView.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        ObservabilityArea.AreaCharacteristics areaCharacteristics = mock(ObservabilityArea.AreaCharacteristics.class);
        Graph graph = mock(Graph.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        Node outsideEndpoint = mock(Node.class);
        EquipmentNode equipmentNode = mock(EquipmentNode.class);
        BusNode localEndpoint = new BusNode("localBus", "localBus", false);
        Connectable<?> connectable = mock(Connectable.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        Terminal.BusView terminalBusView = mock(Terminal.BusView.class);
        VoltageLevel otherVoltageLevel = mock(VoltageLevel.class);

        when(equipmentNode.getEquipmentId()).thenReturn("equipment");
        when(equipmentNode.getAdjacentNodes()).thenReturn(List.of(localEndpoint));
        when(graph.getVoltageLevelGraph(outsideEndpoint)).thenReturn(null);
        when(graph.getVoltageLevelGraph(equipmentNode)).thenReturn(voltageLevelGraph);
        when(graph.getVoltageLevelGraph(localEndpoint)).thenReturn(voltageLevelGraph);
        when(voltageLevelGraph.getVoltageLevelInfos()).thenReturn(new VoltageLevelInfos("localVl", "localVl", 400));
        doReturn(connectable).when(network).getIdentifiable("equipment");
        doReturn(List.of(terminal1, terminal2)).when(connectable).getTerminals();
        when(terminal1.getVoltageLevel()).thenReturn(voltageLevel);
        when(terminal2.getVoltageLevel()).thenReturn(otherVoltageLevel);
        when(terminal1.getBusView()).thenReturn(terminalBusView);
        when(terminalBusView.getBus()).thenReturn(bus);
        when(voltageLevel.getId()).thenReturn("localVl");
        when(otherVoltageLevel.getId()).thenReturn("otherVl");
        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("localBus")).thenReturn(bus);
        when(bus.getId()).thenReturn("localBus");
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityArea.isConsistentWithTopology()).thenReturn(true);
        when(observabilityBusView.getObservabilityArea("localBus")).thenReturn(areaCharacteristics);
        when(areaCharacteristics.getStatus()).thenReturn(ObservabilityArea.ObservabilityStatus.OBSERVABLE);

        Edge edge = new Edge(outsideEndpoint, equipmentNode);

        assertThat(new ObservabilityStyleProvider(network).getEdgeStyles(graph, edge))
                .containsExactly("sld-observability-observable");
    }

    @Test
    void nodeStyleUsesBusInDiagramForMultiTerminalEquipment() {
        Network network = mock(Network.class);
        Network.BusView busView = mock(Network.BusView.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        ObservabilityArea.AreaCharacteristics areaCharacteristics = mock(ObservabilityArea.AreaCharacteristics.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        EquipmentNode equipmentNode = mock(EquipmentNode.class);
        BusNode localEndpoint = new BusNode("localBus", "localBus", false);
        Connectable<?> connectable = mock(Connectable.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        Terminal.BusView terminalBusView = mock(Terminal.BusView.class);
        VoltageLevel otherVoltageLevel = mock(VoltageLevel.class);

        when(equipmentNode.getEquipmentId()).thenReturn("equipment");
        when(equipmentNode.getAdjacentNodes()).thenReturn(List.of(localEndpoint));
        when(voltageLevelGraph.getVoltageLevelGraph(equipmentNode)).thenReturn(voltageLevelGraph);
        when(voltageLevelGraph.getVoltageLevelGraph(localEndpoint)).thenReturn(voltageLevelGraph);
        when(voltageLevelGraph.getVoltageLevelInfos()).thenReturn(new VoltageLevelInfos("localVl", "localVl", 400));
        doReturn(connectable).when(network).getIdentifiable("equipment");
        doReturn(List.of(terminal1, terminal2)).when(connectable).getTerminals();
        when(terminal1.getVoltageLevel()).thenReturn(voltageLevel);
        when(terminal2.getVoltageLevel()).thenReturn(otherVoltageLevel);
        when(terminal1.getBusView()).thenReturn(terminalBusView);
        when(terminalBusView.getBus()).thenReturn(bus);
        when(voltageLevel.getId()).thenReturn("localVl");
        when(otherVoltageLevel.getId()).thenReturn("otherVl");
        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("localBus")).thenReturn(bus);
        when(bus.getId()).thenReturn("localBus");
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityArea.isConsistentWithTopology()).thenReturn(true);
        when(observabilityBusView.getObservabilityArea("localBus")).thenReturn(areaCharacteristics);
        when(areaCharacteristics.getStatus()).thenReturn(ObservabilityArea.ObservabilityStatus.OBSERVABLE);

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, equipmentNode, null, false))
                .containsExactly("sld-observability-observable");
    }

    @Test
    void edgeStyleDoesNotFallbackToAnotherEndpointWhenLocalEndpointIsAmbiguous() {
        Network network = mock(Network.class);
        Network.BusView busView = mock(Network.BusView.class);
        Bus unrelatedBus = mock(Bus.class);
        VoltageLevel unrelatedVoltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        ObservabilityArea.AreaCharacteristics areaCharacteristics = mock(ObservabilityArea.AreaCharacteristics.class);
        Graph graph = mock(Graph.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        EquipmentNode localEndpoint = mock(EquipmentNode.class);
        BusNode unrelatedEndpoint = new BusNode("unrelatedBus", "unrelatedBus", false);
        Connectable<?> connectable = mock(Connectable.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        VoltageLevel localVoltageLevel = mock(VoltageLevel.class);

        when(localEndpoint.getEquipmentId()).thenReturn("equipment");
        when(localEndpoint.getAdjacentNodes()).thenReturn(List.of(unrelatedEndpoint));
        when(graph.getVoltageLevelGraph(localEndpoint)).thenReturn(voltageLevelGraph);
        when(graph.getVoltageLevelGraph(unrelatedEndpoint)).thenReturn(voltageLevelGraph);
        when(voltageLevelGraph.getVoltageLevelInfos()).thenReturn(new VoltageLevelInfos("localVl", "localVl", 400));
        doReturn(connectable).when(network).getIdentifiable("equipment");
        doReturn(List.of(terminal1, terminal2)).when(connectable).getTerminals();
        when(terminal1.getVoltageLevel()).thenReturn(localVoltageLevel);
        when(terminal2.getVoltageLevel()).thenReturn(localVoltageLevel);
        when(localVoltageLevel.getId()).thenReturn("localVl");
        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("unrelatedBus")).thenReturn(unrelatedBus);
        when(unrelatedBus.getVoltageLevel()).thenReturn(unrelatedVoltageLevel);
        when(unrelatedVoltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityBusView.getObservabilityArea("unrelatedBus")).thenReturn(areaCharacteristics);
        when(areaCharacteristics.getStatus()).thenReturn(ObservabilityArea.ObservabilityStatus.OBSERVABLE);

        Edge edge = new Edge(localEndpoint, unrelatedEndpoint);

        assertThat(new ObservabilityStyleProvider(network).getEdgeStyles(graph, edge))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void nodeStyleUsesBusConnectedToBusbarSection() {
        Network network = mock(Network.class);
        Network.BusView busView = mock(Network.BusView.class);
        BusbarSection busbarSection = mock(BusbarSection.class);
        Terminal terminal = mock(Terminal.class);
        Terminal.BusView terminalBusView = mock(Terminal.BusView.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        ObservabilityArea.AreaCharacteristics areaCharacteristics = mock(ObservabilityArea.AreaCharacteristics.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busbarNode = new BusNode("busbarSection", "busbarSection", false);

        when(network.getBusbarSection("busbarSection")).thenReturn(busbarSection);
        when(busbarSection.getTerminal()).thenReturn(terminal);
        when(terminal.getBusView()).thenReturn(terminalBusView);
        when(terminalBusView.getBus()).thenReturn(bus);
        when(bus.getId()).thenReturn("networkBus");
        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("networkBus")).thenReturn(bus);
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityArea.isConsistentWithTopology()).thenReturn(true);
        when(observabilityBusView.getObservabilityArea("networkBus")).thenReturn(areaCharacteristics);
        when(areaCharacteristics.getStatus()).thenReturn(ObservabilityArea.ObservabilityStatus.OBSERVABLE);

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busbarNode, null, false))
                .containsExactly("sld-observability-observable");
    }

    @Test
    void edgeStyleUsesBusConnectedToBusbarSection() {
        Network network = mock(Network.class);
        Network.BusView busView = mock(Network.BusView.class);
        BusbarSection busbarSection = mock(BusbarSection.class);
        Terminal terminal = mock(Terminal.class);
        Terminal.BusView terminalBusView = mock(Terminal.BusView.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        ObservabilityArea.AreaCharacteristics areaCharacteristics = mock(ObservabilityArea.AreaCharacteristics.class);
        Graph graph = mock(Graph.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        Node outsideEndpoint = mock(Node.class);
        BusNode busbarNode = new BusNode("busbarSection", "busbarSection", false);

        when(network.getBusbarSection("busbarSection")).thenReturn(busbarSection);
        when(busbarSection.getTerminal()).thenReturn(terminal);
        when(terminal.getBusView()).thenReturn(terminalBusView);
        when(terminalBusView.getBus()).thenReturn(bus);
        when(bus.getId()).thenReturn("networkBus");
        when(graph.getVoltageLevelGraph(outsideEndpoint)).thenReturn(null);
        when(graph.getVoltageLevelGraph(busbarNode)).thenReturn(voltageLevelGraph);
        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("networkBus")).thenReturn(bus);
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityArea.isConsistentWithTopology()).thenReturn(true);
        when(observabilityBusView.getObservabilityArea("networkBus")).thenReturn(areaCharacteristics);
        when(areaCharacteristics.getStatus()).thenReturn(ObservabilityArea.ObservabilityStatus.OBSERVABLE);

        Edge edge = new Edge(outsideEndpoint, busbarNode);

        assertThat(new ObservabilityStyleProvider(network).getEdgeStyles(graph, edge))
                .containsExactly("sld-observability-observable");
    }

    @Test
    void repeatedNodeStylingUsesCachedBusAndObservabilityStyle() {
        Network network = mock(Network.class);
        Network.BusView busView = mock(Network.BusView.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        ObservabilityArea.AreaCharacteristics areaCharacteristics = mock(ObservabilityArea.AreaCharacteristics.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("bus", "bus", false);

        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("bus")).thenReturn(bus);
        when(bus.getId()).thenReturn("bus");
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityArea.isConsistentWithTopology()).thenReturn(true);
        when(observabilityBusView.getObservabilityArea("bus")).thenReturn(areaCharacteristics);
        when(areaCharacteristics.getStatus()).thenReturn(ObservabilityArea.ObservabilityStatus.OBSERVABLE);

        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        assertThat(provider.getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-observable");
        assertThat(provider.getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-observable");

        verify(network, times(1)).getBusbarSection("bus");
        verify(busView, times(1)).getBus("bus");
        verify(voltageLevel, times(1)).getExtension(ObservabilityArea.class);
        verify(observabilityBusView, times(1)).getObservabilityArea("bus");
    }

    @Test
    void repeatedUnresolvedNodeStylingUsesCachedNoInformationResult() {
        Network network = mock(Network.class);
        Network.BusView busView = mock(Network.BusView.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("missingBus", "missingBus", false);

        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("missingBus")).thenReturn(null);

        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        assertThat(provider.getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");
        assertThat(provider.getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");

        verify(network, times(1)).getBusbarSection("missingBus");
        verify(busView, times(1)).getBus("missingBus");
    }

    @Test
    void missingObservabilityExtensionReturnsNoInformation() {
        Network network = mock(Network.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("bus", "bus", false);

        configureBusLookup(network, bus, "bus");
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void missingObservabilityBusViewReturnsNoInformation() {
        Network network = mock(Network.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("bus", "bus", false);

        configureBusLookup(network, bus, "bus");
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void missingObservabilityAreaReturnsNoInformation() {
        Network network = mock(Network.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("bus", "bus", false);

        configureBusLookup(network, bus, "bus");
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityBusView.getObservabilityArea("bus")).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void observabilityAreaLookupFailureReturnsNoInformation() {
        Network network = mock(Network.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("bus", "bus", false);

        configureBusLookup(network, bus, "bus");
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityArea.isConsistentWithTopology()).thenReturn(true);
        when(observabilityBusView.getObservabilityArea("bus"))
                .thenThrow(new PowsyblException("bus is not present in the observability snapshot"));

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void missingObservabilityStatusReturnsNoInformation() {
        Network network = mock(Network.class);
        Bus bus = mock(Bus.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("bus", "bus", false);

        configureBusLookup(network, bus, "bus");
        configureObservability(bus, "bus", null);

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void inconsistentObservabilityAreaReturnsNoInformation() {
        Network network = mock(Network.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("bus", "bus", false);

        configureBusLookup(network, bus, "bus");
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityArea.isConsistentWithTopology()).thenReturn(false);

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");
        verify(observabilityBusView, never()).getObservabilityArea("bus");
    }

    @Test
    void observabilityExtensionLookupFailureReturnsNoInformation() {
        Network network = mock(Network.class);
        Bus bus = mock(Bus.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("bus", "bus", false);

        configureBusLookup(network, bus, "bus");
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class))
                .thenThrow(new PowsyblException("observability extension is unavailable"));

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void busWithoutVoltageLevelReturnsNoInformation() {
        Network network = mock(Network.class);
        Bus bus = mock(Bus.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("bus", "bus", false);

        configureBusLookup(network, bus, "bus");
        when(bus.getVoltageLevel()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void unsupportedWindingSubcomponentsAreNotStyled() {
        Network network = mock(Network.class);
        Middle2WTNode transformerNode = mock(Middle2WTNode.class);
        Node unrelatedNode = mock(Node.class);
        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING3")).isEmpty();
        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "UNKNOWN")).isEmpty();
        assertThat(provider.getNodeSubcomponentStyles(null, unrelatedNode, "WINDING1")).isEmpty();
    }

    @Test
    void equipmentSubcomponentsUseEquipmentBus() {
        Network network = mock(Network.class);
        EquipmentNode equipmentNode = mock(EquipmentNode.class);
        Graph graph = mock(Graph.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        VoltageLevelInfos voltageLevelInfos = mock(VoltageLevelInfos.class);
        Connectable<?> connectable = mock(Connectable.class);
        Terminal terminal = mock(Terminal.class);
        Terminal.BusView terminalBusView = mock(Terminal.BusView.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        Bus bus = mock(Bus.class);
        List<String> equipmentSubcomponents = List.of(
                "LOAD", "BATTERY", "GENERATOR", "CAPACITOR", "INDUCTOR",
                "STATIC_VAR_COMPENSATOR", "VSC_CONVERTER_STATION", "LCC_CONVERTER_STATION",
                "BUS_CONNECTION", "NODE", "GROUND");

        when(equipmentNode.getEquipmentId()).thenReturn("equipment");
        when(graph.getVoltageLevelGraph(equipmentNode)).thenReturn(voltageLevelGraph);
        when(voltageLevelGraph.getVoltageLevelInfos()).thenReturn(voltageLevelInfos);
        when(voltageLevelInfos.id()).thenReturn("voltageLevel");
        doReturn(connectable).when(network).getIdentifiable("equipment");
        doReturn(List.of(terminal)).when(connectable).getTerminals();
        when(terminal.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getId()).thenReturn("voltageLevel");
        when(terminal.getBusView()).thenReturn(terminalBusView);
        when(terminalBusView.getBus()).thenReturn(bus);
        configureObservability(bus, "bus", ObservabilityArea.ObservabilityStatus.OBSERVABLE);

        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        for (String subcomponent : equipmentSubcomponents) {
            assertThat(provider.getNodeSubcomponentStyles(graph, equipmentNode, subcomponent))
                    .as("subcomponent %s", subcomponent)
                    .containsExactly("sld-observability-observable");
        }
    }

    @Test
    void excludedEquipmentSubcomponentsRemainUnstyled() {
        Network network = mock(Network.class);
        EquipmentNode equipmentNode = mock(EquipmentNode.class);
        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        for (String subcomponent : List.of("CLOSED", "OPEN", "ATTACH", "UP", "DOWN", "FLASH", "LOCK")) {
            assertThat(provider.getNodeSubcomponentStyles(null, equipmentNode, subcomponent))
                    .as("subcomponent %s", subcomponent)
                    .isEmpty();
        }
    }

    @Test
    void missingTwoWindingTransformerReturnsNoInformationForWinding() {
        Network network = mock(Network.class);
        Middle2WTNode transformerNode = mock(Middle2WTNode.class);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getTwoWindingsTransformer("transformer")).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void missingThreeWindingTransformerReturnsNoInformationForWinding() {
        Network network = mock(Network.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void missingTransformerTerminalBusReturnsNoInformationForWinding() {
        Network network = mock(Network.class);
        TwoWindingsTransformer transformer = mock(TwoWindingsTransformer.class);
        Middle2WTNode transformerNode = mock(Middle2WTNode.class);
        Terminal terminal = mock(Terminal.class);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getTwoWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformer.getTerminal1()).thenReturn(terminal);
        when(terminal.getBusView()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void equipmentNodeWithoutVoltageLevelGraphReturnsNoInformation() {
        Network network = mock(Network.class);
        EquipmentNode equipmentNode = mock(EquipmentNode.class);

        when(equipmentNode.getEquipmentId()).thenReturn("equipment");

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(null, equipmentNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void equipmentNodeWithMissingVoltageLevelInfosReturnsNoInformation() {
        Network network = mock(Network.class);
        EquipmentNode equipmentNode = mock(EquipmentNode.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);

        when(equipmentNode.getEquipmentId()).thenReturn("equipment");
        when(voltageLevelGraph.getVoltageLevelInfos()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeStyles(voltageLevelGraph, equipmentNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void equipmentNodeWithMissingVoltageLevelIdReturnsNoInformation() {
        Network network = mock(Network.class);
        EquipmentNode equipmentNode = mock(EquipmentNode.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        VoltageLevelInfos voltageLevelInfos = mock(VoltageLevelInfos.class);

        when(equipmentNode.getEquipmentId()).thenReturn("equipment");
        when(voltageLevelGraph.getVoltageLevelInfos()).thenReturn(voltageLevelInfos);
        when(voltageLevelInfos.id()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeStyles(voltageLevelGraph, equipmentNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void equipmentNodeWithUnknownIdentifiableReturnsNoInformation() {
        Network network = mock(Network.class);
        EquipmentNode equipmentNode = mock(EquipmentNode.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        VoltageLevelInfos voltageLevelInfos = mock(VoltageLevelInfos.class);

        when(equipmentNode.getEquipmentId()).thenReturn("equipment");
        when(voltageLevelGraph.getVoltageLevelInfos()).thenReturn(voltageLevelInfos);
        when(voltageLevelInfos.id()).thenReturn("voltageLevel");
        when(network.getIdentifiable("equipment")).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeStyles(voltageLevelGraph, equipmentNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void internalEquipmentNodeReturnsNoInformation() {
        Network network = mock(Network.class);
        EquipmentNode equipmentNode = mock(EquipmentNode.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);

        when(equipmentNode.getType()).thenReturn(Node.NodeType.INTERNAL);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeStyles(voltageLevelGraph, equipmentNode, null, false))
                .containsExactly("sld-observability-no-information");
        verifyNoInteractions(network);
    }

    @Test
    void connectedNodeUsesItsSingleConnectedBus() {
        Network network = mock(Network.class);
        Node equipmentNode = mock(Node.class);
        Bus bus = mock(Bus.class);
        BusNode busNode = new BusNode("bus", "bus", false);
        configureBusLookup(network, bus, "bus");
        configureObservability(bus, "bus", ObservabilityArea.ObservabilityStatus.OBSERVABLE);
        when(equipmentNode.getAdjacentNodes()).thenReturn(List.of(busNode));

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeStyles(null, equipmentNode, null, false))
                .containsExactly("sld-observability-observable");
    }

    @Test
    void twoWindingSubcomponentsUseTheirTransformerTerminals() {
        Network network = mock(Network.class);
        TwoWindingsTransformer transformer = mock(TwoWindingsTransformer.class);
        Middle2WTNode transformerNode = mock(Middle2WTNode.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        Terminal.BusView terminalBusView1 = mock(Terminal.BusView.class);
        Terminal.BusView terminalBusView2 = mock(Terminal.BusView.class);
        Bus bus1 = mock(Bus.class);
        Bus bus2 = mock(Bus.class);
        configureObservability(bus1, "bus1", ObservabilityArea.ObservabilityStatus.OBSERVABLE);
        configureObservability(bus2, "bus2", ObservabilityArea.ObservabilityStatus.NON_OBSERVABLE);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getTwoWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformer.getTerminal1()).thenReturn(terminal1);
        when(transformer.getTerminal2()).thenReturn(terminal2);
        when(terminal1.getBusView()).thenReturn(terminalBusView1);
        when(terminal2.getBusView()).thenReturn(terminalBusView2);
        when(terminalBusView1.getBus()).thenReturn(bus1);
        when(terminalBusView2.getBus()).thenReturn(bus2);

        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-observable");
        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING2"))
                .containsExactly("sld-observability-non-observable");
        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING3"))
                .isEmpty();
    }

    @Test
    void voltageLevelTwoWindingSubcomponentsUseTheirTransformerTerminals() {
        Network network = mock(Network.class);
        TwoWindingsTransformer transformer = mock(TwoWindingsTransformer.class);
        FeederNode transformerNode = mock(FeederNode.class);
        Graph graph = mock(Graph.class);
        VoltageLevelGraph localVoltageLevelGraph = mock(VoltageLevelGraph.class);
        VoltageLevelInfos localVoltageLevelInfos = mock(VoltageLevelInfos.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        Terminal.BusView terminalBusView1 = mock(Terminal.BusView.class);
        Terminal.BusView terminalBusView2 = mock(Terminal.BusView.class);
        Bus bus1 = mock(Bus.class);
        Bus bus2 = mock(Bus.class);
        VoltageLevel voltageLevel1 = mock(VoltageLevel.class);
        VoltageLevel voltageLevel2 = mock(VoltageLevel.class);
        configureObservability(bus1, "bus1", ObservabilityArea.ObservabilityStatus.OBSERVABLE);
        configureObservability(bus2, "bus2", ObservabilityArea.ObservabilityStatus.NON_OBSERVABLE);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(graph.getVoltageLevelGraph(transformerNode)).thenReturn(localVoltageLevelGraph);
        when(localVoltageLevelGraph.getVoltageLevelInfos()).thenReturn(localVoltageLevelInfos);
        when(localVoltageLevelInfos.id()).thenReturn("voltageLevel2");
        when(network.getTwoWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformer.getTerminal1()).thenReturn(terminal1);
        when(transformer.getTerminal2()).thenReturn(terminal2);
        when(terminal1.getVoltageLevel()).thenReturn(voltageLevel1);
        when(terminal2.getVoltageLevel()).thenReturn(voltageLevel2);
        when(voltageLevel1.getId()).thenReturn("voltageLevel1");
        when(voltageLevel2.getId()).thenReturn("voltageLevel2");
        when(terminal1.getBusView()).thenReturn(terminalBusView1);
        when(terminal2.getBusView()).thenReturn(terminalBusView2);
        when(terminalBusView1.getBus()).thenReturn(bus1);
        when(terminalBusView2.getBus()).thenReturn(bus2);

        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        assertThat(provider.getNodeSubcomponentStyles(graph, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-non-observable");
        assertThat(provider.getNodeSubcomponentStyles(graph, transformerNode, "WINDING2"))
                .containsExactly("sld-observability-observable");
    }

    @Test
    void threeWindingSubcomponentsUseTheirTransformerLegs() {
        Network network = mock(Network.class);
        ThreeWindingsTransformer transformer = mock(ThreeWindingsTransformer.class);
        ThreeWindingsTransformer.Leg leg1 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg2 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg3 = mock(ThreeWindingsTransformer.Leg.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        Terminal terminal3 = mock(Terminal.class);
        Terminal.BusView terminalBusView1 = mock(Terminal.BusView.class);
        Terminal.BusView terminalBusView2 = mock(Terminal.BusView.class);
        Terminal.BusView terminalBusView3 = mock(Terminal.BusView.class);
        Bus bus1 = mock(Bus.class);
        Bus bus2 = mock(Bus.class);
        Bus bus3 = mock(Bus.class);
        configureObservability(bus1, "bus1", ObservabilityArea.ObservabilityStatus.OBSERVABLE);
        configureObservability(bus2, "bus2", ObservabilityArea.ObservabilityStatus.NON_OBSERVABLE);
        configureObservability(bus3, "bus3", ObservabilityArea.ObservabilityStatus.BORDER);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformer.getLeg1()).thenReturn(leg1);
        when(transformer.getLeg2()).thenReturn(leg2);
        when(transformer.getLeg3()).thenReturn(leg3);
        when(leg1.getTerminal()).thenReturn(terminal1);
        when(leg2.getTerminal()).thenReturn(terminal2);
        when(leg3.getTerminal()).thenReturn(terminal3);
        when(terminal1.getBusView()).thenReturn(terminalBusView1);
        when(terminal2.getBusView()).thenReturn(terminalBusView2);
        when(terminal3.getBusView()).thenReturn(terminalBusView3);
        when(terminalBusView1.getBus()).thenReturn(bus1);
        when(terminalBusView2.getBus()).thenReturn(bus2);
        when(terminalBusView3.getBus()).thenReturn(bus3);

        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-observable");
        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING2"))
                .containsExactly("sld-observability-non-observable");
        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING3"))
                .containsExactly("sld-observability-border");
    }

    @Test
    void threeWindingSubcomponentFallsBackToItsLegWithoutVisualMetadata() {
        Network network = mock(Network.class);
        ThreeWindingsTransformer transformer = mock(ThreeWindingsTransformer.class);
        ThreeWindingsTransformer.Leg leg1 = mock(ThreeWindingsTransformer.Leg.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);
        Terminal terminal = mock(Terminal.class);
        Terminal.BusView terminalBusView = mock(Terminal.BusView.class);
        Bus bus = mock(Bus.class);
        configureObservability(bus, "bus", ObservabilityArea.ObservabilityStatus.OBSERVABLE);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformer.getLeg1()).thenReturn(leg1);
        when(leg1.getTerminal()).thenReturn(terminal);
        when(terminal.getBusView()).thenReturn(terminalBusView);
        when(terminalBusView.getBus()).thenReturn(bus);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-observable");
    }

    @Test
    void threeWindingFeederSubcomponentsUseTheirLegTerminalsWithoutGraph() {
        Network network = mock(Network.class);
        ThreeWindingsTransformer transformer = mock(ThreeWindingsTransformer.class);
        ThreeWindingsTransformer.Leg leg1 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg2 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg3 = mock(ThreeWindingsTransformer.Leg.class);
        FeederNode transformerNode = mock(FeederNode.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        Terminal terminal3 = mock(Terminal.class);
        Terminal.BusView terminalBusView1 = mock(Terminal.BusView.class);
        Terminal.BusView terminalBusView2 = mock(Terminal.BusView.class);
        Terminal.BusView terminalBusView3 = mock(Terminal.BusView.class);
        Bus bus1 = mock(Bus.class);
        Bus bus2 = mock(Bus.class);
        Bus bus3 = mock(Bus.class);
        configureObservability(bus1, "bus1", ObservabilityArea.ObservabilityStatus.OBSERVABLE);
        configureObservability(bus2, "bus2", ObservabilityArea.ObservabilityStatus.NON_OBSERVABLE);
        configureObservability(bus3, "bus3", ObservabilityArea.ObservabilityStatus.BORDER);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getTwoWindingsTransformer("transformer")).thenReturn(null);
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformer.getLeg1()).thenReturn(leg1);
        when(transformer.getLeg2()).thenReturn(leg2);
        when(transformer.getLeg3()).thenReturn(leg3);
        when(leg1.getTerminal()).thenReturn(terminal1);
        when(leg2.getTerminal()).thenReturn(terminal2);
        when(leg3.getTerminal()).thenReturn(terminal3);
        when(terminal1.getBusView()).thenReturn(terminalBusView1);
        when(terminal2.getBusView()).thenReturn(terminalBusView2);
        when(terminal3.getBusView()).thenReturn(terminalBusView3);
        when(terminalBusView1.getBus()).thenReturn(bus1);
        when(terminalBusView2.getBus()).thenReturn(bus2);
        when(terminalBusView3.getBus()).thenReturn(bus3);

        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-observable");
        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING2"))
                .containsExactly("sld-observability-non-observable");
        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING3"))
                .containsExactly("sld-observability-border");
    }

    @Test
    void nullNetworkIsRejected() {
        assertThatThrownBy(() -> new ObservabilityStyleProvider(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void threeWindingSubcomponentsFollowTheirVisualWindingPositions() {
        Network network = mock(Network.class);
        ThreeWindingsTransformer transformer = mock(ThreeWindingsTransformer.class);
        ThreeWindingsTransformer.Leg leg1 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg2 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg3 = mock(ThreeWindingsTransformer.Leg.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);
        VoltageLevelInfos voltageLevel1Infos = mock(VoltageLevelInfos.class);
        VoltageLevelInfos voltageLevel2Infos = mock(VoltageLevelInfos.class);
        VoltageLevelInfos voltageLevel3Infos = mock(VoltageLevelInfos.class);
        VoltageLevel voltageLevel1 = mock(VoltageLevel.class);
        VoltageLevel voltageLevel2 = mock(VoltageLevel.class);
        VoltageLevel voltageLevel3 = mock(VoltageLevel.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        Terminal terminal3 = mock(Terminal.class);
        Terminal.BusView terminalBusView1 = mock(Terminal.BusView.class);
        Terminal.BusView terminalBusView2 = mock(Terminal.BusView.class);
        Terminal.BusView terminalBusView3 = mock(Terminal.BusView.class);
        Bus bus1 = mock(Bus.class);
        Bus bus2 = mock(Bus.class);
        Bus bus3 = mock(Bus.class);
        configureObservability(bus1, "bus1", ObservabilityArea.ObservabilityStatus.OBSERVABLE);
        configureObservability(bus2, "bus2", ObservabilityArea.ObservabilityStatus.NON_OBSERVABLE);
        configureObservability(bus3, "bus3", ObservabilityArea.ObservabilityStatus.BORDER);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformer.getLeg1()).thenReturn(leg1);
        when(transformer.getLeg2()).thenReturn(leg2);
        when(transformer.getLeg3()).thenReturn(leg3);
        when(leg1.getTerminal()).thenReturn(terminal1);
        when(leg2.getTerminal()).thenReturn(terminal2);
        when(leg3.getTerminal()).thenReturn(terminal3);
        when(transformerNode.getVoltageLevelInfos(Middle3WTNode.Winding.UPPER_LEFT)).thenReturn(voltageLevel2Infos);
        when(transformerNode.getVoltageLevelInfos(Middle3WTNode.Winding.UPPER_RIGHT)).thenReturn(voltageLevel1Infos);
        when(transformerNode.getVoltageLevelInfos(Middle3WTNode.Winding.DOWN)).thenReturn(voltageLevel3Infos);
        when(voltageLevel1Infos.id()).thenReturn("voltageLevel1");
        when(voltageLevel2Infos.id()).thenReturn("voltageLevel2");
        when(voltageLevel3Infos.id()).thenReturn("voltageLevel3");
        when(terminal1.getVoltageLevel()).thenReturn(voltageLevel1);
        when(terminal2.getVoltageLevel()).thenReturn(voltageLevel2);
        when(terminal3.getVoltageLevel()).thenReturn(voltageLevel3);
        when(voltageLevel1.getId()).thenReturn("voltageLevel1");
        when(voltageLevel2.getId()).thenReturn("voltageLevel2");
        when(voltageLevel3.getId()).thenReturn("voltageLevel3");
        when(terminal1.getBusView()).thenReturn(terminalBusView1);
        when(terminal2.getBusView()).thenReturn(terminalBusView2);
        when(terminal3.getBusView()).thenReturn(terminalBusView3);
        when(terminalBusView1.getBus()).thenReturn(bus1);
        when(terminalBusView2.getBus()).thenReturn(bus2);
        when(terminalBusView3.getBus()).thenReturn(bus3);

        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-non-observable");
        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING2"))
                .containsExactly("sld-observability-observable");
        assertThat(provider.getNodeSubcomponentStyles(null, transformerNode, "WINDING3"))
                .containsExactly("sld-observability-border");
    }

    @Test
    void staleThreeWindingVisualMetadataReturnsNoInformation() {
        Network network = mock(Network.class);
        ThreeWindingsTransformer transformer = mock(ThreeWindingsTransformer.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);
        VoltageLevelInfos voltageLevelInfos = mock(VoltageLevelInfos.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        Terminal terminal3 = mock(Terminal.class);
        VoltageLevel voltageLevel1 = mock(VoltageLevel.class);
        VoltageLevel voltageLevel2 = mock(VoltageLevel.class);
        VoltageLevel voltageLevel3 = mock(VoltageLevel.class);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformerNode.getVoltageLevelInfos(Middle3WTNode.Winding.UPPER_LEFT)).thenReturn(voltageLevelInfos);
        when(voltageLevelInfos.id()).thenReturn("stale-voltage-level");
        ThreeWindingsTransformer.Leg leg1 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg2 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg3 = mock(ThreeWindingsTransformer.Leg.class);
        when(transformer.getLeg1()).thenReturn(leg1);
        when(transformer.getLeg2()).thenReturn(leg2);
        when(transformer.getLeg3()).thenReturn(leg3);
        when(leg1.getTerminal()).thenReturn(terminal1);
        when(leg2.getTerminal()).thenReturn(terminal2);
        when(leg3.getTerminal()).thenReturn(terminal3);
        when(terminal1.getVoltageLevel()).thenReturn(voltageLevel1);
        when(terminal2.getVoltageLevel()).thenReturn(voltageLevel2);
        when(terminal3.getVoltageLevel()).thenReturn(voltageLevel3);
        when(voltageLevel1.getId()).thenReturn("voltage-level-1");
        when(voltageLevel2.getId()).thenReturn("voltage-level-2");
        when(voltageLevel3.getId()).thenReturn("voltage-level-3");

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void threeWindingVisualMetadataWithoutVoltageLevelIdReturnsNoInformation() {
        Network network = mock(Network.class);
        ThreeWindingsTransformer transformer = mock(ThreeWindingsTransformer.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);
        VoltageLevelInfos voltageLevelInfos = mock(VoltageLevelInfos.class);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformerNode.getVoltageLevelInfos(Middle3WTNode.Winding.UPPER_LEFT)).thenReturn(voltageLevelInfos);
        when(voltageLevelInfos.id()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void threeWindingVisualMetadataWithSeveralMatchingTerminalsReturnsNoInformation() {
        Network network = mock(Network.class);
        ThreeWindingsTransformer transformer = mock(ThreeWindingsTransformer.class);
        ThreeWindingsTransformer.Leg leg1 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg2 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg3 = mock(ThreeWindingsTransformer.Leg.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);
        VoltageLevelInfos voltageLevelInfos = mock(VoltageLevelInfos.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformerNode.getVoltageLevelInfos(Middle3WTNode.Winding.UPPER_LEFT)).thenReturn(voltageLevelInfos);
        when(voltageLevelInfos.id()).thenReturn("voltageLevel");
        when(transformer.getLeg1()).thenReturn(leg1);
        when(transformer.getLeg2()).thenReturn(leg2);
        when(transformer.getLeg3()).thenReturn(leg3);
        when(leg1.getTerminal()).thenReturn(terminal1);
        when(leg2.getTerminal()).thenReturn(terminal2);
        when(leg3.getTerminal()).thenReturn(null);
        when(terminal1.getVoltageLevel()).thenReturn(voltageLevel);
        when(terminal2.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getId()).thenReturn("voltageLevel");

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(null, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void threeWindingTransformerEdgeUsesExternalLegBus() {
        Network network = mock(Network.class);
        ThreeWindingsTransformer transformer = mock(ThreeWindingsTransformer.class);
        ThreeWindingsTransformer.Leg leg1 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg2 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg3 = mock(ThreeWindingsTransformer.Leg.class);
        FeederNode feederNode = mock(FeederNode.class);
        FeederTwLeg feeder = mock(FeederTwLeg.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);
        Graph graph = mock(Graph.class);
        VoltageLevelGraph localVoltageLevelGraph = mock(VoltageLevelGraph.class);
        VoltageLevelInfos localVoltageLevelInfos = mock(VoltageLevelInfos.class);
        VoltageLevelInfos externalVoltageLevelInfos = mock(VoltageLevelInfos.class);
        VoltageLevel localVoltageLevel = mock(VoltageLevel.class);
        VoltageLevel externalVoltageLevel = mock(VoltageLevel.class);
        Terminal localTerminal = mock(Terminal.class);
        Terminal externalTerminal = mock(Terminal.class);
        Terminal thirdTerminal = mock(Terminal.class);
        Terminal.BusView localBusView = mock(Terminal.BusView.class);
        Terminal.BusView externalBusView = mock(Terminal.BusView.class);
        Bus localBus = mock(Bus.class);
        Bus externalBus = mock(Bus.class);
        configureObservability(localBus, "localBus", ObservabilityArea.ObservabilityStatus.OBSERVABLE);
        configureObservability(externalBus, "externalBus", ObservabilityArea.ObservabilityStatus.NON_OBSERVABLE);

        when(feederNode.getEquipmentId()).thenReturn("transformer");
        when(feederNode.getFeeder()).thenReturn(feeder);
        when(feeder.getSide()).thenReturn(NodeSide.TWO);
        when(feeder.getOtherSideVoltageLevelInfos()).thenReturn(externalVoltageLevelInfos);
        when(graph.getVoltageLevelGraph(feederNode)).thenReturn(localVoltageLevelGraph);
        when(localVoltageLevelGraph.getVoltageLevelInfos()).thenReturn(localVoltageLevelInfos);
        when(localVoltageLevelInfos.id()).thenReturn("localVoltageLevel");
        doReturn(transformer).when(network).getIdentifiable("transformer");
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(transformer);
        doReturn(List.of(localTerminal, externalTerminal, thirdTerminal)).when(transformer).getTerminals();
        when(transformer.getLeg1()).thenReturn(leg1);
        when(transformer.getLeg2()).thenReturn(leg2);
        when(transformer.getLeg3()).thenReturn(leg3);
        when(leg1.getTerminal()).thenReturn(localTerminal);
        when(leg2.getTerminal()).thenReturn(externalTerminal);
        when(leg3.getTerminal()).thenReturn(thirdTerminal);
        when(localTerminal.getVoltageLevel()).thenReturn(localVoltageLevel);
        when(externalTerminal.getVoltageLevel()).thenReturn(externalVoltageLevel);
        when(thirdTerminal.getVoltageLevel()).thenReturn(externalVoltageLevel);
        when(localVoltageLevel.getId()).thenReturn("localVoltageLevel");
        when(externalVoltageLevel.getId()).thenReturn("externalVoltageLevel");
        when(localTerminal.getBusView()).thenReturn(localBusView);
        when(externalTerminal.getBusView()).thenReturn(externalBusView);
        when(localBusView.getBus()).thenReturn(localBus);
        when(externalBusView.getBus()).thenReturn(externalBus);

        Edge edge = new Edge(feederNode, transformerNode);

        assertThat(new ObservabilityStyleProvider(network).getEdgeStyles(graph, edge))
                .containsExactly("sld-observability-non-observable");
    }

    @Test
    void threeWindingTransformerEdgeUsesFirstAndThirdLegBuses() {
        Network network = mock(Network.class);
        ThreeWindingsTransformer transformer = mock(ThreeWindingsTransformer.class);
        ThreeWindingsTransformer.Leg leg1 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg2 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg3 = mock(ThreeWindingsTransformer.Leg.class);
        FeederNode feederNode = mock(FeederNode.class);
        FeederTwLeg feeder = mock(FeederTwLeg.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);
        VoltageLevelInfos externalVoltageLevelInfos = mock(VoltageLevelInfos.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        Terminal terminal3 = mock(Terminal.class);
        Terminal.BusView terminalBusView1 = mock(Terminal.BusView.class);
        Terminal.BusView terminalBusView2 = mock(Terminal.BusView.class);
        Terminal.BusView terminalBusView3 = mock(Terminal.BusView.class);
        Bus bus1 = mock(Bus.class);
        Bus bus2 = mock(Bus.class);
        Bus bus3 = mock(Bus.class);
        configureObservability(bus1, "bus1", ObservabilityArea.ObservabilityStatus.OBSERVABLE);
        configureObservability(bus2, "bus2", ObservabilityArea.ObservabilityStatus.NON_OBSERVABLE);
        configureObservability(bus3, "bus3", ObservabilityArea.ObservabilityStatus.BORDER);

        when(feederNode.getEquipmentId()).thenReturn("transformer");
        when(feederNode.getFeeder()).thenReturn(feeder);
        when(feeder.getOtherSideVoltageLevelInfos()).thenReturn(externalVoltageLevelInfos);
        when(feeder.getSide()).thenReturn(NodeSide.ONE, NodeSide.THREE);
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(transformer);
        when(transformer.getLeg1()).thenReturn(leg1);
        when(transformer.getLeg2()).thenReturn(leg2);
        when(transformer.getLeg3()).thenReturn(leg3);
        when(leg1.getTerminal()).thenReturn(terminal1);
        when(leg2.getTerminal()).thenReturn(terminal2);
        when(leg3.getTerminal()).thenReturn(terminal3);
        when(terminal1.getBusView()).thenReturn(terminalBusView1);
        when(terminal2.getBusView()).thenReturn(terminalBusView2);
        when(terminal3.getBusView()).thenReturn(terminalBusView3);
        when(terminalBusView1.getBus()).thenReturn(bus1);
        when(terminalBusView2.getBus()).thenReturn(bus2);
        when(terminalBusView3.getBus()).thenReturn(bus3);

        Edge edge = new Edge(feederNode, transformerNode);
        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        assertThat(provider.getEdgeStyles(mock(Graph.class), edge))
                .containsExactly("sld-observability-observable");
        assertThat(provider.getEdgeStyles(mock(Graph.class), edge))
                .containsExactly("sld-observability-border");
    }

    @Test
    void threeWindingTransformerEdgeWithoutExternalFeederContextReturnsNoInformation() {
        Network network = mock(Network.class);
        Graph graph = mock(Graph.class);
        FeederNode feederNode = mock(FeederNode.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);

        when(feederNode.getFeeder()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getEdgeStyles(graph, new Edge(feederNode, transformerNode)))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void threeWindingTransformerEdgeWithUnknownTransformerReturnsNoInformation() {
        Network network = mock(Network.class);
        Graph graph = mock(Graph.class);
        FeederNode feederNode = mock(FeederNode.class);
        FeederTwLeg feeder = mock(FeederTwLeg.class);
        Middle3WTNode transformerNode = mock(Middle3WTNode.class);
        VoltageLevelInfos externalVoltageLevelInfos = mock(VoltageLevelInfos.class);

        when(feederNode.getEquipmentId()).thenReturn("transformer");
        when(feederNode.getFeeder()).thenReturn(feeder);
        when(feeder.getOtherSideVoltageLevelInfos()).thenReturn(externalVoltageLevelInfos);
        when(network.getThreeWindingsTransformer("transformer")).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getEdgeStyles(graph, new Edge(feederNode, transformerNode)))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void edgeWithoutVoltageLevelEndpointReturnsNoInformation() {
        Network network = mock(Network.class);
        Graph graph = mock(Graph.class);
        Node node1 = mock(Node.class);
        Node node2 = mock(Node.class);

        when(graph.getVoltageLevelGraph(node1)).thenReturn(null);
        when(graph.getVoltageLevelGraph(node2)).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network).getEdgeStyles(graph, new Edge(node1, node2)))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void edgeWithNullGraphReturnsNoInformation() {
        Network network = mock(Network.class);
        Node node1 = mock(Node.class);
        Node node2 = mock(Node.class);

        assertThat(new ObservabilityStyleProvider(network).getEdgeStyles(null, new Edge(node1, node2)))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void busNodeWithoutNetworkBusViewReturnsNoInformation() {
        Network network = mock(Network.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busNode = new BusNode("bus", "bus", false);

        when(network.getBusView()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void busbarSectionWithoutTerminalBusReturnsNoInformation() {
        Network network = mock(Network.class);
        BusbarSection busbarSection = mock(BusbarSection.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        BusNode busbarNode = new BusNode("busbarSection", "busbarSection", false);

        when(network.getBusbarSection("busbarSection")).thenReturn(busbarSection);
        when(busbarSection.getTerminal()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(voltageLevelGraph, busbarNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void connectedNodesOnDifferentBusesReturnNoInformation() {
        Network network = mock(Network.class);
        Network.BusView busView = mock(Network.BusView.class);
        Bus bus1 = mock(Bus.class);
        Bus bus2 = mock(Bus.class);
        Node equipmentNode = mock(Node.class);
        BusNode busNode1 = new BusNode("bus1", "bus1", false);
        BusNode busNode2 = new BusNode("bus2", "bus2", false);

        when(equipmentNode.getAdjacentNodes()).thenReturn(List.of(busNode1, busNode2));
        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("bus1")).thenReturn(bus1);
        when(busView.getBus("bus2")).thenReturn(bus2);
        when(bus1.getId()).thenReturn("bus1");
        when(bus2.getId()).thenReturn("bus2");

        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(null, equipmentNode, null, false))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void breakerRemainsUnstyled() {
        Network network = mock(Network.class);
        BusNode busNode = new BusNode("bus", "bus", false);
        SwitchNode switchNode = new SwitchNode("switch", "switch", "BREAKER", false,
                SwitchNode.SwitchKind.BREAKER, false);
        Edge edge = new Edge(switchNode, busNode);
        switchNode.addAdjacentEdge(edge);
        busNode.addAdjacentEdge(edge);
        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(null, switchNode, null, false))
                .isEmpty();
    }

    @Test
    void openBreakerRemainsUnstyled() {
        Network network = mock(Network.class);
        BusNode busNode = new BusNode("bus", "bus", false);
        SwitchNode switchNode = new SwitchNode("switch", "switch", "BREAKER", false,
                SwitchNode.SwitchKind.BREAKER, true);
        Edge edge = new Edge(switchNode, busNode);
        switchNode.addAdjacentEdge(edge);
        busNode.addAdjacentEdge(edge);
        assertThat(new ObservabilityStyleProvider(network).getNodeStyles(null, switchNode, null, false))
                .isEmpty();
    }

    @Test
    void switchAndGroundDisconnectionEquipmentSubcomponentsRemainUnstyled() {
        Network network = mock(Network.class);
        SwitchNode switchNode = new SwitchNode("switch", "switch", "BREAKER", false,
                SwitchNode.SwitchKind.BREAKER, false);
        GroundDisconnectionNode groundDisconnectionNode = new GroundDisconnectionNode(
                "groundDisconnection", "groundDisconnection", false, "GROUND_DISCONNECTION");
        ObservabilityStyleProvider provider = new ObservabilityStyleProvider(network);

        assertThat(provider.getNodeSubcomponentStyles(null, switchNode, "LOAD")).isEmpty();
        assertThat(provider.getNodeSubcomponentStyles(null, groundDisconnectionNode, "GROUND")).isEmpty();
    }

    @Test
    void voltageLevelWindingWithoutLocalGraphReturnsNoInformation() {
        Network network = mock(Network.class);
        TwoWindingsTransformer transformer = mock(TwoWindingsTransformer.class);
        FeederNode transformerNode = mock(FeederNode.class);
        Graph graph = mock(Graph.class);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getTwoWindingsTransformer("transformer")).thenReturn(transformer);
        when(graph.getVoltageLevelGraph(transformerNode)).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(graph, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void voltageLevelWindingWithoutVoltageLevelInfosReturnsNoInformation() {
        Network network = mock(Network.class);
        TwoWindingsTransformer transformer = mock(TwoWindingsTransformer.class);
        FeederNode transformerNode = mock(FeederNode.class);
        Graph graph = mock(Graph.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getTwoWindingsTransformer("transformer")).thenReturn(transformer);
        when(graph.getVoltageLevelGraph(transformerNode)).thenReturn(voltageLevelGraph);
        when(voltageLevelGraph.getVoltageLevelInfos()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(graph, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void voltageLevelWindingWithoutVoltageLevelIdReturnsNoInformation() {
        Network network = mock(Network.class);
        TwoWindingsTransformer transformer = mock(TwoWindingsTransformer.class);
        FeederNode transformerNode = mock(FeederNode.class);
        Graph graph = mock(Graph.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        VoltageLevelInfos voltageLevelInfos = mock(VoltageLevelInfos.class);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getTwoWindingsTransformer("transformer")).thenReturn(transformer);
        when(graph.getVoltageLevelGraph(transformerNode)).thenReturn(voltageLevelGraph);
        when(voltageLevelGraph.getVoltageLevelInfos()).thenReturn(voltageLevelInfos);
        when(voltageLevelInfos.id()).thenReturn(null);

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(graph, transformerNode, "WINDING1"))
                .containsExactly("sld-observability-no-information");
    }

    @Test
    void voltageLevelWindingWithAmbiguousLocalTerminalsReturnsNoInformation() {
        Network network = mock(Network.class);
        TwoWindingsTransformer transformer = mock(TwoWindingsTransformer.class);
        FeederNode transformerNode = mock(FeederNode.class);
        Graph graph = mock(Graph.class);
        VoltageLevelGraph voltageLevelGraph = mock(VoltageLevelGraph.class);
        VoltageLevelInfos voltageLevelInfos = mock(VoltageLevelInfos.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        VoltageLevel voltageLevel1 = mock(VoltageLevel.class);
        VoltageLevel voltageLevel2 = mock(VoltageLevel.class);

        when(transformerNode.getEquipmentId()).thenReturn("transformer");
        when(network.getTwoWindingsTransformer("transformer")).thenReturn(transformer);
        when(graph.getVoltageLevelGraph(transformerNode)).thenReturn(voltageLevelGraph);
        when(voltageLevelGraph.getVoltageLevelInfos()).thenReturn(voltageLevelInfos);
        when(voltageLevelInfos.id()).thenReturn("voltageLevel");
        when(transformer.getTerminal1()).thenReturn(terminal1);
        when(transformer.getTerminal2()).thenReturn(terminal2);
        when(terminal1.getVoltageLevel()).thenReturn(voltageLevel1);
        when(terminal2.getVoltageLevel()).thenReturn(voltageLevel2);
        when(voltageLevel1.getId()).thenReturn("voltageLevel");
        when(voltageLevel2.getId()).thenReturn("voltageLevel");

        assertThat(new ObservabilityStyleProvider(network)
                .getNodeSubcomponentStyles(graph, transformerNode, "WINDING2"))
                .containsExactly("sld-observability-no-information");
    }

    private void configureObservability(Bus bus, String busId, ObservabilityArea.ObservabilityStatus status) {
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        ObservabilityArea observabilityArea = mock(ObservabilityArea.class);
        ObservabilityArea.BusView observabilityBusView = mock(ObservabilityArea.BusView.class);
        ObservabilityArea.AreaCharacteristics areaCharacteristics = mock(ObservabilityArea.AreaCharacteristics.class);

        when(bus.getId()).thenReturn(busId);
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getExtension(ObservabilityArea.class)).thenReturn(observabilityArea);
        when(observabilityArea.getBusView()).thenReturn(observabilityBusView);
        when(observabilityArea.isConsistentWithTopology()).thenReturn(true);
        when(observabilityBusView.getObservabilityArea(busId)).thenReturn(areaCharacteristics);
        when(areaCharacteristics.getStatus()).thenReturn(status);
    }

    private void configureBusLookup(Network network, Bus bus, String busId) {
        Network.BusView busView = mock(Network.BusView.class);

        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus(busId)).thenReturn(bus);
        when(bus.getId()).thenReturn(busId);
    }
}
