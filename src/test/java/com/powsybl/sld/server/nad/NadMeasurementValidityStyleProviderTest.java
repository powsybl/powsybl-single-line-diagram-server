/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.nad;

import com.powsybl.commons.config.BaseVoltagesConfig;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.Measurement;
import com.powsybl.iidm.network.extensions.Measurements;
import com.powsybl.nad.model.BranchEdge;
import com.powsybl.nad.model.BusNode;
import com.powsybl.nad.model.ThreeWtEdge;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NadMeasurementValidityStyleProviderTest {

    private Network mockEmptyNetwork() {
        Network network = mock(Network.class);
        when(network.getLines()).thenReturn(List.of());
        when(network.getTwoWindingsTransformers()).thenReturn(List.of());
        when(network.getThreeWindingsTransformers()).thenReturn(List.of());
        return network;
    }

    private Measurements<?> mockActivePowerMeasurement(double value) {
        Measurements<?> measurements = mock(Measurements.class);
        Measurement measurement = mock(Measurement.class);
        when(measurement.getValue()).thenReturn(value);
        when(measurements.getMeasurements(Measurement.Type.ACTIVE_POWER)).thenReturn(List.of(measurement));
        return measurements;
    }

    private <B extends Branch<B>> void mockConnectedBranch(Network network, B branch, String id) {
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        when(voltageLevel.getNominalV()).thenReturn(400.0);
        when(terminal1.isConnected()).thenReturn(true);
        when(terminal2.isConnected()).thenReturn(true);
        when(terminal1.getVoltageLevel()).thenReturn(voltageLevel);
        when(terminal2.getVoltageLevel()).thenReturn(voltageLevel);
        when(branch.getTerminal(TwoSides.ONE)).thenReturn(terminal1);
        when(branch.getTerminal(TwoSides.TWO)).thenReturn(terminal2);
        when(branch.isOverloaded()).thenReturn(false);
        doReturn(branch).when(network).getBranch(id);
    }

    @Test
    void testHighPtmLineIsRed() {
        Network network = mockEmptyNetwork();
        Line highPtmLine = mock(Line.class);
        when(highPtmLine.getId()).thenReturn("LINE1");
        Measurements<?> measurements = mockActivePowerMeasurement(25.0);
        when(highPtmLine.getExtension(Measurements.class)).thenReturn(measurements);
        when(network.getLines()).thenReturn(List.of(highPtmLine));
        mockConnectedBranch(network, highPtmLine, "LINE1");

        NadMeasurementValidityStyleProvider provider = new NadMeasurementValidityStyleProvider(network, new BaseVoltagesConfig());
        BranchEdge branchEdge = new BranchEdge(id -> id, "LINE1", "LINE1", BranchEdge.LINE_EDGE, null, null, null);

        List<String> styles = provider.getBranchEdgeStyleClasses(branchEdge);
        assertTrue(styles.contains(NadMeasurementValidityStyleProvider.HIGH_PTM_BRANCH_CLASS));
        assertFalse(styles.contains(NadMeasurementValidityStyleProvider.LOW_PTM_BRANCH_CLASS));
    }

    @Test
    void testLowPtmLineIsGreen() {
        Network network = mockEmptyNetwork();
        Line lowPtmLine = mock(Line.class);
        when(lowPtmLine.getId()).thenReturn("LINE2");
        Measurements<?> measurements = mockActivePowerMeasurement(5.0);
        when(lowPtmLine.getExtension(Measurements.class)).thenReturn(measurements);
        when(network.getLines()).thenReturn(List.of(lowPtmLine));
        mockConnectedBranch(network, lowPtmLine, "LINE2");

        NadMeasurementValidityStyleProvider provider = new NadMeasurementValidityStyleProvider(network, new BaseVoltagesConfig());
        BranchEdge branchEdge = new BranchEdge(id -> id, "LINE2", "LINE2", BranchEdge.LINE_EDGE, null, null, null);

        List<String> styles = provider.getBranchEdgeStyleClasses(branchEdge);
        assertTrue(styles.contains(NadMeasurementValidityStyleProvider.LOW_PTM_BRANCH_CLASS));
        assertFalse(styles.contains(NadMeasurementValidityStyleProvider.HIGH_PTM_BRANCH_CLASS));
    }

    @Test
    void testTwoWindingsTransformerIsAlsoClassified() {
        Network network = mockEmptyNetwork();
        TwoWindingsTransformer twt = mock(TwoWindingsTransformer.class);
        when(twt.getId()).thenReturn("TWT1");
        Measurements<?> measurements = mockActivePowerMeasurement(30.0);
        when(twt.getExtension(Measurements.class)).thenReturn(measurements);
        when(network.getTwoWindingsTransformers()).thenReturn(List.of(twt));
        mockConnectedBranch(network, twt, "TWT1");

        NadMeasurementValidityStyleProvider provider = new NadMeasurementValidityStyleProvider(network, new BaseVoltagesConfig());
        BranchEdge branchEdge = new BranchEdge(id -> id, "TWT1", "TWT1", BranchEdge.TWO_WT_EDGE, null, null, null);

        assertTrue(provider.getBranchEdgeStyleClasses(branchEdge).contains(NadMeasurementValidityStyleProvider.HIGH_PTM_BRANCH_CLASS));
    }

    @Test
    void testThreeWindingsTransformerIsClassifiedViaThreeWtEdgeStyleClasses() {
        Network network = mockEmptyNetwork();
        ThreeWindingsTransformer twt = mock(ThreeWindingsTransformer.class);
        when(twt.getId()).thenReturn("TWT3W");
        Measurements<?> measurements = mockActivePowerMeasurement(2.0);
        when(twt.getExtension(Measurements.class)).thenReturn(measurements);
        when(network.getThreeWindingsTransformers()).thenReturn(List.of(twt));
        when(network.getThreeWindingsTransformer("TWT3W")).thenReturn(twt);

        NadMeasurementValidityStyleProvider provider = new NadMeasurementValidityStyleProvider(network, new BaseVoltagesConfig());
        ThreeWtEdge threeWtEdge = new ThreeWtEdge(id -> id, "TWT3W", "TWT3W", ThreeWtEdge.Side.ONE, ThreeWtEdge.THREE_WT_EDGE, true, null);

        assertTrue(provider.getThreeWtEdgeStyleClasses(threeWtEdge).contains(NadMeasurementValidityStyleProvider.LOW_PTM_BRANCH_CLASS));
    }

    @Test
    void testBranchConnectedToInvalidVoltageBusGetsInvalidClass() {
        Network network = mockEmptyNetwork();
        Line line = mock(Line.class);
        doReturn(line).when(network).getConnectable("LINE3");
        mockConnectedBranch(network, line, "LINE3");

        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        doReturn(List.of(terminal1, terminal2)).when(line).getTerminals();

        Bus invalidBus = mock(Bus.class);
        BusbarSection busbarSection = mock(BusbarSection.class);
        Terminal bbsTerminal = mock(Terminal.class);
        Measurements<?> voltageMeasurements = mock(Measurements.class);
        Measurement measurement = mock(Measurement.class);
        when(measurement.isValid()).thenReturn(false);
        when(voltageMeasurements.getMeasurements(Measurement.Type.VOLTAGE)).thenReturn(List.of(measurement));
        when(busbarSection.getExtension(Measurements.class)).thenReturn(voltageMeasurements);
        when(bbsTerminal.getConnectable()).thenReturn(busbarSection);
        when(invalidBus.getConnectedTerminalStream()).thenAnswer(invocation -> Stream.of(bbsTerminal));

        Terminal.BusView busView1 = mock(Terminal.BusView.class);
        when(busView1.getBus()).thenReturn(invalidBus);
        when(terminal1.getBusView()).thenReturn(busView1);
        Terminal.BusView busView2 = mock(Terminal.BusView.class);
        when(terminal2.getBusView()).thenReturn(busView2);

        NadMeasurementValidityStyleProvider provider = new NadMeasurementValidityStyleProvider(network, new BaseVoltagesConfig());
        BranchEdge branchEdge = new BranchEdge(id -> id, "LINE3", "LINE3", BranchEdge.LINE_EDGE, null, null, null);

        assertTrue(provider.getBranchEdgeStyleClasses(branchEdge).contains(NadMeasurementValidityStyleProvider.INVALID_VOLTAGE_BRANCH_CLASS));
    }

    @Test
    void testBusConnectedToHighPtmLineIsRed() {
        Network network = mockEmptyNetwork();
        Line highPtmLine = mock(Line.class);
        when(highPtmLine.getId()).thenReturn("LINE1");
        Measurements<?> measurements = mockActivePowerMeasurement(25.0);
        when(highPtmLine.getExtension(Measurements.class)).thenReturn(measurements);
        when(network.getLines()).thenReturn(List.of(highPtmLine));

        mockBusConnectedTo(network, highPtmLine);

        NadMeasurementValidityStyleProvider provider = new NadMeasurementValidityStyleProvider(network, new BaseVoltagesConfig());
        BusNode busNode = new BusNode("BUS1", "BUS1", Collections.emptyList(), "");

        List<String> styles = provider.getBusNodeStyleClasses(busNode);
        assertTrue(styles.contains(NadMeasurementValidityStyleProvider.HIGH_PTM_BUS_CLASS));
        assertFalse(styles.contains(NadMeasurementValidityStyleProvider.LOW_PTM_BUS_CLASS));
    }

    @Test
    void testBusConnectedToLowPtmLineIsGreen() {
        Network network = mockEmptyNetwork();
        Line lowPtmLine = mock(Line.class);
        when(lowPtmLine.getId()).thenReturn("LINE2");
        Measurements<?> measurements = mockActivePowerMeasurement(5.0);
        when(lowPtmLine.getExtension(Measurements.class)).thenReturn(measurements);
        when(network.getLines()).thenReturn(List.of(lowPtmLine));

        mockBusConnectedTo(network, lowPtmLine);

        NadMeasurementValidityStyleProvider provider = new NadMeasurementValidityStyleProvider(network, new BaseVoltagesConfig());
        BusNode busNode = new BusNode("BUS1", "BUS1", Collections.emptyList(), "");

        List<String> styles = provider.getBusNodeStyleClasses(busNode);
        assertTrue(styles.contains(NadMeasurementValidityStyleProvider.LOW_PTM_BUS_CLASS));
        assertFalse(styles.contains(NadMeasurementValidityStyleProvider.HIGH_PTM_BUS_CLASS));
    }

    @Test
    void testBusWithValidVoltageMeasurementOnBusbarSectionIsMarkedValid() {
        Network network = mockEmptyNetwork();
        Bus bus = mock(Bus.class);
        Network.BusView busView = mock(Network.BusView.class);
        BusbarSection busbarSection = mock(BusbarSection.class);
        Terminal terminal = mock(Terminal.class);
        Measurements<?> voltageMeasurements = mock(Measurements.class);
        Measurement measurement = mock(Measurement.class);

        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("BUS1")).thenReturn(bus);
        when(bus.getConnectedTerminalStream()).thenAnswer(invocation -> Stream.of(terminal));
        when(terminal.getConnectable()).thenReturn(busbarSection);
        when(busbarSection.getExtension(Measurements.class)).thenReturn(voltageMeasurements);
        when(voltageMeasurements.getMeasurements(Measurement.Type.VOLTAGE)).thenReturn(List.of(measurement));
        when(measurement.isValid()).thenReturn(true);
        stubVoltageWithinLimits(bus);

        NadMeasurementValidityStyleProvider provider = new NadMeasurementValidityStyleProvider(network, new BaseVoltagesConfig());
        BusNode busNode = new BusNode("BUS1", "BUS1", Collections.emptyList(), "");

        List<String> styles = provider.getBusNodeStyleClasses(busNode);
        assertTrue(styles.contains(NadMeasurementValidityStyleProvider.VOLTAGE_VALID_CLASS));
        assertFalse(styles.contains(NadMeasurementValidityStyleProvider.VOLTAGE_INVALID_CLASS));
    }

    @Test
    void testBusWithInvalidVoltageMeasurementOnBusbarSectionIsMarkedInvalid() {
        Network network = mockEmptyNetwork();
        Bus bus = mock(Bus.class);
        Network.BusView busView = mock(Network.BusView.class);
        BusbarSection busbarSection = mock(BusbarSection.class);
        Terminal terminal = mock(Terminal.class);
        Measurements<?> voltageMeasurements = mock(Measurements.class);
        Measurement measurement = mock(Measurement.class);

        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("BUS1")).thenReturn(bus);
        when(bus.getConnectedTerminalStream()).thenAnswer(invocation -> Stream.of(terminal));
        when(terminal.getConnectable()).thenReturn(busbarSection);
        when(busbarSection.getExtension(Measurements.class)).thenReturn(voltageMeasurements);
        when(voltageMeasurements.getMeasurements(Measurement.Type.VOLTAGE)).thenReturn(List.of(measurement));
        when(measurement.isValid()).thenReturn(false);
        stubVoltageWithinLimits(bus);

        NadMeasurementValidityStyleProvider provider = new NadMeasurementValidityStyleProvider(network, new BaseVoltagesConfig());
        BusNode busNode = new BusNode("BUS1", "BUS1", Collections.emptyList(), "");

        List<String> styles = provider.getBusNodeStyleClasses(busNode);
        assertTrue(styles.contains(NadMeasurementValidityStyleProvider.VOLTAGE_INVALID_CLASS));
        assertFalse(styles.contains(NadMeasurementValidityStyleProvider.VOLTAGE_VALID_CLASS));
    }

    @Test
    void testEdgeInfoStyleClassesForMeasurementValidity() {
        NadMeasurementValidityStyleProvider provider = new NadMeasurementValidityStyleProvider(mockEmptyNetwork(), new BaseVoltagesConfig());

        assertTrue(provider.getEdgeInfoStyleClasses(NadMeasurementValidityLabelProvider.MEASUREMENT_VALID)
                .contains(NadMeasurementValidityStyleProvider.MEASUREMENT_VALID_CLASS));
        assertTrue(provider.getEdgeInfoStyleClasses(NadMeasurementValidityLabelProvider.MEASUREMENT_INVALID)
                .contains(NadMeasurementValidityStyleProvider.MEASUREMENT_INVALID_CLASS));
    }

    private void mockBusConnectedTo(Network network, Line line) {
        Bus bus = mock(Bus.class);
        Terminal terminal = mock(Terminal.class);
        Network.BusView busView = mock(Network.BusView.class);

        when(network.getBusView()).thenReturn(busView);
        when(busView.getBus("BUS1")).thenReturn(bus);
        when(terminal.getConnectable()).thenReturn(line);
        when(bus.getConnectedTerminalStream()).thenAnswer(invocation -> Stream.of(terminal));
        stubVoltageWithinLimits(bus);
    }

    private void stubVoltageWithinLimits(Bus bus) {
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        when(bus.getVoltageLevel()).thenReturn(voltageLevel);
        when(voltageLevel.getHighVoltageLimit()).thenReturn(1000.0);
        when(voltageLevel.getLowVoltageLimit()).thenReturn(0.0);
        when(bus.getV()).thenReturn(400.0);
    }
}
