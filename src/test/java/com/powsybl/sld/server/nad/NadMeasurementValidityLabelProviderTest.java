/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.nad;

import com.powsybl.diagram.util.ValueFormatter;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.Measurement;
import com.powsybl.iidm.network.extensions.Measurements;
import com.powsybl.nad.model.BranchEdge;
import com.powsybl.nad.svg.EdgeInfo;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.VoltageLevelLegend;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NadMeasurementValidityLabelProviderTest {

    private final ValueFormatter valueFormatter = new SvgParameters().createValueFormatter();

    private Network mockNetwork;
    private Measurements<?> measurements;

    private <B extends Branch<B>> B setUpBranch(Class<B> branchClass, String id) {
        mockNetwork = mock(Network.class);
        B branch = mock(branchClass);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);

        when(mockNetwork.getBranch(id)).thenReturn(branch);
        when(branch.getTerminal(TwoSides.ONE)).thenReturn(terminal1);
        when(branch.getTerminal(TwoSides.TWO)).thenReturn(terminal2);
        when(branch.getTerminal1()).thenReturn(terminal1);
        when(branch.getTerminal2()).thenReturn(terminal2);
        when(terminal1.getP()).thenReturn(50.0);
        when(terminal2.getP()).thenReturn(-30.0);
        when(terminal1.isConnected()).thenReturn(true);
        when(terminal2.isConnected()).thenReturn(true);
        when(branch.getCurrentLimits(TwoSides.ONE)).thenReturn(Optional.empty());
        when(branch.getCurrentLimits(TwoSides.TWO)).thenReturn(Optional.empty());

        measurements = mock(Measurements.class);
        when(branch.getExtension(Measurements.class)).thenReturn(measurements);
        when(measurements.getMeasurements(any(Measurement.Type.class))).thenReturn(List.of());
        return branch;
    }

    private NadMeasurementValidityLabelProvider newProvider() {
        SvgParameters svgParameters = mock(SvgParameters.class);
        when(svgParameters.createValueFormatter()).thenReturn(valueFormatter);
        return new NadMeasurementValidityLabelProvider(mockNetwork, svgParameters);
    }

    private Measurement mockMeasurement(double value, boolean valid) {
        Measurement measurement = mock(Measurement.class);
        when(measurement.getValue()).thenReturn(value);
        when(measurement.isValid()).thenReturn(valid);
        return measurement;
    }

    private void stubMeasurement(Measurements<?> targetMeasurements, Measurement.Type type, double value, boolean valid) {
        Measurement measurement = mockMeasurement(value, valid);
        when(targetMeasurements.getMeasurements(type)).thenReturn(List.of(measurement));
    }

    @Test
    void testValidPAndInvalidQMeasurementsReplaceBothLabels() {
        setUpBranch(Line.class, "LINE1");
        stubMeasurement(measurements, Measurement.Type.ACTIVE_POWER, 25.0, true);
        stubMeasurement(measurements, Measurement.Type.REACTIVE_POWER, -12.0, false);

        EdgeInfo edgeInfo = newProvider().getBranchEdgeInfo("LINE1", "LINE").orElseThrow();

        assertEquals(NadMeasurementValidityLabelProvider.MEASUREMENT_VALID, edgeInfo.getInfoTypeA());
        assertEquals(valueFormatter.formatPowerWithAbs(25.0, ""), edgeInfo.getLabelA().orElse(""));
        assertEquals(NadMeasurementValidityLabelProvider.MEASUREMENT_INVALID, edgeInfo.getInfoTypeB());
        assertEquals(valueFormatter.formatPowerWithAbs(-12.0, ""), edgeInfo.getLabelB().orElse(""));
    }

    @Test
    void testOnlyActivePowerMeasurementKeepsBaseReactiveSide() {
        setUpBranch(Line.class, "LINE1");
        stubMeasurement(measurements, Measurement.Type.ACTIVE_POWER, 25.0, true);

        EdgeInfo edgeInfo = newProvider().getBranchEdgeInfo("LINE1", "LINE").orElseThrow();

        assertEquals(NadMeasurementValidityLabelProvider.MEASUREMENT_VALID, edgeInfo.getInfoTypeA());
        assertEquals(EdgeInfo.VALUE_PERMANENT_LIMIT_PERCENTAGE, edgeInfo.getInfoTypeB());
    }

    @Test
    void testPerSideBranchEdgeInfoIsSuppressed() {
        setUpBranch(Line.class, "LINE1");

        Optional<EdgeInfo> edgeInfo = newProvider().getBranchEdgeInfo("LINE1", BranchEdge.Side.ONE, "LINE");

        assertTrue(edgeInfo.isEmpty());
    }

    @Test
    void testNoMeasurementFallsBackToBaseInfo() {
        setUpBranch(Line.class, "LINE1");

        EdgeInfo edgeInfo = newProvider().getBranchEdgeInfo("LINE1", "LINE").orElseThrow();

        assertEquals(EdgeInfo.ACTIVE_POWER, edgeInfo.getInfoTypeA());
        assertEquals("50", edgeInfo.getLabelA().orElse(""));
    }

    @Test
    void testTwoWindingsTransformerIsAlsoDecorated() {
        setUpBranch(TwoWindingsTransformer.class, "TWT1");
        stubMeasurement(measurements, Measurement.Type.ACTIVE_POWER, 40.0, true);
        stubMeasurement(measurements, Measurement.Type.REACTIVE_POWER, 15.0, true);

        EdgeInfo edgeInfo = newProvider().getBranchEdgeInfo("TWT1", "TWO_WT").orElseThrow();

        assertEquals(NadMeasurementValidityLabelProvider.MEASUREMENT_VALID, edgeInfo.getInfoTypeA());
        assertEquals(valueFormatter.formatPowerWithAbs(40.0, ""), edgeInfo.getLabelA().orElse(""));
        assertEquals(NadMeasurementValidityLabelProvider.MEASUREMENT_VALID, edgeInfo.getInfoTypeB());
        assertEquals(valueFormatter.formatPowerWithAbs(15.0, ""), edgeInfo.getLabelB().orElse(""));
    }

    @Test
    void testThreeWindingsTransformerIsDecorated() {
        mockNetwork = mock(Network.class);
        ThreeWindingsTransformer twt = mock(ThreeWindingsTransformer.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        Terminal terminal3 = mock(Terminal.class);
        ThreeWindingsTransformer.Leg leg1 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg2 = mock(ThreeWindingsTransformer.Leg.class);
        ThreeWindingsTransformer.Leg leg3 = mock(ThreeWindingsTransformer.Leg.class);

        when(mockNetwork.getThreeWindingsTransformer("TWT3W")).thenReturn(twt);
        when(twt.getTerminal(ThreeSides.ONE)).thenReturn(terminal1);
        when(twt.getTerminal(ThreeSides.TWO)).thenReturn(terminal2);
        when(twt.getTerminal(ThreeSides.THREE)).thenReturn(terminal3);
        when(terminal1.isConnected()).thenReturn(true);
        when(twt.getLeg1()).thenReturn(leg1);
        when(twt.getLeg(ThreeSides.ONE)).thenReturn(leg1);
        when(twt.getLeg(ThreeSides.TWO)).thenReturn(leg2);
        when(twt.getLeg(ThreeSides.THREE)).thenReturn(leg3);
        when(leg1.getTerminal()).thenReturn(terminal1);
        when(leg1.getCurrentLimits()).thenReturn(Optional.empty());
        when(leg2.getCurrentLimits()).thenReturn(Optional.empty());
        when(leg3.getCurrentLimits()).thenReturn(Optional.empty());

        Measurements<?> twtMeasurements = mock(Measurements.class);
        when(twt.getExtension(Measurements.class)).thenReturn(twtMeasurements);
        when(twtMeasurements.getMeasurements(any(Measurement.Type.class))).thenReturn(List.of());
        stubMeasurement(twtMeasurements, Measurement.Type.ACTIVE_POWER, 18.0, true);
        stubMeasurement(twtMeasurements, Measurement.Type.REACTIVE_POWER, -6.0, false);

        EdgeInfo edgeInfo = newProvider().getThreeWindingTransformerEdgeInfo("TWT3W", com.powsybl.nad.model.ThreeWtEdge.Side.ONE).orElseThrow();

        assertEquals(NadMeasurementValidityLabelProvider.MEASUREMENT_VALID, edgeInfo.getInfoTypeA());
        assertEquals(valueFormatter.formatPowerWithAbs(18.0, ""), edgeInfo.getLabelA().orElse(""));
        assertEquals(NadMeasurementValidityLabelProvider.MEASUREMENT_INVALID, edgeInfo.getInfoTypeB());
        assertEquals(valueFormatter.formatPowerWithAbs(-6.0, ""), edgeInfo.getLabelB().orElse(""));
    }

    @Test
    void testVoltageLevelLegendReplacesKvWithTmVoltageFromBusbarSection() {
        mockNetwork = mock(Network.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        VoltageLevel.BusView vlBusView = mock(VoltageLevel.BusView.class);
        Network.BusView networkBusView = mock(Network.BusView.class);
        Bus bus = mock(Bus.class);
        Terminal terminal = mock(Terminal.class);
        BusbarSection busbarSection = mock(BusbarSection.class);
        Measurements<?> bbsMeasurements = mock(Measurements.class);

        when(mockNetwork.getVoltageLevel("VL1")).thenReturn(voltageLevel);
        when(voltageLevel.getBusView()).thenReturn(vlBusView);
        when(vlBusView.getBuses()).thenReturn(List.of(bus));
        when(mockNetwork.getBusView()).thenReturn(networkBusView);
        when(networkBusView.getBus("BUS1")).thenReturn(bus);
        when(bus.getId()).thenReturn("BUS1");
        when(bus.getV()).thenReturn(399.0);
        when(bus.getAngle()).thenReturn(0.0);
        when(bus.getConnectedTerminalStream()).thenAnswer(invocation -> Stream.of(terminal));
        when(terminal.getConnectable()).thenReturn(busbarSection);
        when(busbarSection.getExtension(Measurements.class)).thenReturn(bbsMeasurements);
        stubMeasurement(bbsMeasurements, Measurement.Type.VOLTAGE, 405.7, true);

        VoltageLevelLegend legend = newProvider().getVoltageLevelLegend("VL1");

        assertEquals(valueFormatter.formatVoltage(405.7, "kV"), legend.getBusLegend("BUS1"));
    }

    @Test
    void testVoltageLevelLegendKeepsBaseValueWhenNoVoltageMeasurement() {
        mockNetwork = mock(Network.class);
        VoltageLevel voltageLevel = mock(VoltageLevel.class);
        VoltageLevel.BusView vlBusView = mock(VoltageLevel.BusView.class);
        Network.BusView networkBusView = mock(Network.BusView.class);
        Bus bus = mock(Bus.class);

        when(mockNetwork.getVoltageLevel("VL1")).thenReturn(voltageLevel);
        when(voltageLevel.getBusView()).thenReturn(vlBusView);
        when(vlBusView.getBuses()).thenReturn(List.of(bus));
        when(mockNetwork.getBusView()).thenReturn(networkBusView);
        when(networkBusView.getBus("BUS1")).thenReturn(bus);
        when(bus.getId()).thenReturn("BUS1");
        when(bus.getV()).thenReturn(399.0);
        when(bus.getAngle()).thenReturn(0.0);
        when(bus.getConnectedTerminalStream()).thenAnswer(invocation -> Stream.empty());

        VoltageLevelLegend legend = newProvider().getVoltageLevelLegend("VL1");

        assertEquals(valueFormatter.formatVoltage(399.0, "kV"), legend.getBusLegend("BUS1"));
    }
}
