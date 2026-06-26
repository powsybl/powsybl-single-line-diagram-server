/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.sld.server.estim;

import com.powsybl.diagram.util.ValueFormatter;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.*;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.model.coordinate.Direction;
import com.powsybl.sld.model.nodes.Feeder;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.model.nodes.FeederType;
import com.powsybl.sld.model.nodes.NodeSide;
import com.powsybl.sld.model.nodes.feeders.FeederWithSides;
import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.SvgParameters;
import com.powsybl.sld.svg.ValueFeederInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
@ExtendWith(MockitoExtension.class)
class StateEstimationLabelProviderTest {

    private static final String REACTIVE_POWER_UNIT = "Mvar";
    private static final String ACTIVE_POWER_UNIT = "MW";
    private static final String CURRENT_UNIT = "A";
    private static final String VOLTAGE_UNIT = "kV";
    private static final String GENERATOR_WITHOUT_MEASUREMENTS = "GEN_NO_MEASUREMENTS";
    private static final String GENERATOR_WITH_MEASUREMENTS = "GEN_WITH_MEASUREMENTS";
    private static final String GENERATOR_WITH_MEASUREMENTS_NO_OBSERVABILITY = "GEN_WITH_MEASUREMENTS_NO_OBSERVABILITY";
    private static final String BRANCH_WITHOUT_MEASUREMENTS = "BRANCH_NO_MEASUREMENTS";
    private static final String BRANCH_WITH_MEASUREMENTS = "BRANCH_WITH_MEASUREMENTS";
    private static final String HVDC_WITHOUT_MEASUREMENTS = "HVDC_NO_MEASUREMENTS";
    private static final String HVDC_WITH_MEASUREMENTS = "HVDC_WITH_MEASUREMENTS";
    private static final double TERMINAL_P = 10D;
    private static final double TERMINAL_Q = 11D;
    private static final double TERMINAL_I = 12D;
    private static final double MEASUREMENT_P = 13D;
    private static final double MEASUREMENT_Q = 14D;
    private static final double MEASUREMENT_V = 15D;
    private static final double MEASUREMENT_P1 = 16D;
    private static final double MEASUREMENT_Q1 = 17D;
    private static final double MEASUREMENT_P2 = 18D;
    private static final double MEASUREMENT_Q2 = 19D;

    @Mock
    private Network networkMock;
    @Mock
    private SldComponentLibrary sldComponentLibraryMock;
    @Mock
    private LayoutParameters layoutParametersMock;
    @Mock
    private SvgParameters svgParametersMock;

    private StateEstimationLabelProvider provider;

    private static Stream<Arguments> provideSidedMeasurements() {
        return Stream.of(
                Arguments.of(NodeSide.ONE, MEASUREMENT_P1, MEASUREMENT_Q1, "sld-measurement-invalid"),
                Arguments.of(NodeSide.TWO, MEASUREMENT_P2, MEASUREMENT_Q2, "sld-measurement-valid sld-measurement-critical")
        );
    }

    @BeforeEach
    void setUp() {
        prepareMocks();
        provider = new StateEstimationLabelProvider(networkMock, sldComponentLibraryMock, layoutParametersMock, svgParametersMock);
    }

    @Test
    void testGetFeederInfosReturnsBaseClassResultForInjectionWithoutMeasurements() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(GENERATOR_WITHOUT_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(3)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
            tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
            tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
            tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null)
        );
    }

    @Test
    void testGetFeederInfosReturnsBaseClassResultAndMeasurementsForInjectionWithMeasurements() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(GENERATOR_WITH_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(6)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
            tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
            tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
            tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null),
            tuple("VALUE_CURRENT", Optional.of(MEASUREMENT_P + " " + ACTIVE_POWER_UNIT), "sld-measurement-valid"),
            tuple("VALUE_CURRENT", Optional.of(MEASUREMENT_Q + " " + REACTIVE_POWER_UNIT), "sld-measurement-invalid sld-measurement-critical"),
            tuple("VALUE_CURRENT", Optional.of(MEASUREMENT_V + " " + VOLTAGE_UNIT), "sld-measurement-invalid")
        );
    }

    @Test
    void testGetFeederInfosReturnsBaseClassResultAndMeasurementsForInjectionWithMeasurementsNoObservability() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(GENERATOR_WITH_MEASUREMENTS_NO_OBSERVABILITY);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(6)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
                        tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
                        tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
                        tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null),
                        tuple("VALUE_CURRENT", Optional.of(MEASUREMENT_P + " " + ACTIVE_POWER_UNIT), "sld-measurement-valid"),
                        tuple("VALUE_CURRENT", Optional.of(MEASUREMENT_Q + " " + REACTIVE_POWER_UNIT), "sld-measurement-invalid"),
                        tuple("VALUE_CURRENT", Optional.of(MEASUREMENT_V + " " + VOLTAGE_UNIT), "sld-measurement-invalid")
        );
    }

    @Test
    void testGetFeederInfosReturnsBaseClassResultForBranchWithNoMeasurements() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.BRANCH);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(BRANCH_WITHOUT_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(4)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
                        tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
                        tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
                        tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null),
                        tuple("VALUE_PERMANENT_LIMIT_PERCENTAGE", Optional.of("0.0 %"), null)
        );
    }

    @ParameterizedTest
    @MethodSource("provideSidedMeasurements")
    void testGetFeederInfosReturnsBaseClassResultAndSidedMeasurementsForBranchWithMeasurements(NodeSide nodeSide, double measurementP, double measurementQ, String userDefinedId) {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.BRANCH);
        Mockito.when(feederMock.getSide()).thenReturn(nodeSide);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(BRANCH_WITH_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(6)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
                        tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
                        tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
                        tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null),
                        tuple("VALUE_PERMANENT_LIMIT_PERCENTAGE", Optional.of("0.0 %"), null),
                        tuple("VALUE_CURRENT", Optional.of(measurementP + " " + ACTIVE_POWER_UNIT), userDefinedId),
                        tuple("VALUE_CURRENT", Optional.of(measurementQ + " " + REACTIVE_POWER_UNIT), userDefinedId)
        );
    }

    @Test
    void testGetFeederInfosReturnsBaseClassResultForHvdcWithNoMeasurements() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.HVDC);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(HVDC_WITHOUT_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(3)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
                tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
                tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
                tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null)
        );
    }

    @ParameterizedTest
    @MethodSource("provideSidedMeasurements")
    void testGetFeederInfosReturnsBaseClassResultAndSidedMeasurementsForHvdcWithMeasurements(NodeSide nodeSide, double measurementP, double measurementQ, String userDefinedId) {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.HVDC);
        Mockito.when(feederMock.getSide()).thenReturn(nodeSide);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(HVDC_WITH_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(5)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
            tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
            tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
            tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null),
            tuple("VALUE_CURRENT", Optional.of(measurementP + " " + ACTIVE_POWER_UNIT), userDefinedId),
            tuple("VALUE_CURRENT", Optional.of(measurementQ + " " + REACTIVE_POWER_UNIT), userDefinedId)
        );
    }

    @ParameterizedTest
    @EnumSource(value = FeederType.class, names = {"INJECTION", "HVDC", "BRANCH"})
    void testGetFeederInfosReturnsEmptyListWhenNonExistingIdentifiable(FeederType feederType) {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(feederType);
        Mockito.lenient().when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn("I DO NOT EXIST");

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).isEmpty();
    }

    @Test
    void testGetFeederInfosRespectsResultRespectsOrderWhenDirectionIsNotBottom() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getDirection()).thenReturn(Direction.TOP);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(GENERATOR_WITH_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos.get(0).getRightLabel()).isEqualTo(Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(1).getRightLabel()).isEqualTo(Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(2).getRightLabel()).isEqualTo(Optional.of(TERMINAL_I + " " + CURRENT_UNIT));
        assertThat(actualFeederInfos.get(3).getRightLabel()).isEqualTo(Optional.of(MEASUREMENT_P + " " + ACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(4).getRightLabel()).isEqualTo(Optional.of(MEASUREMENT_Q + " " + REACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(5).getRightLabel()).isEqualTo(Optional.of(MEASUREMENT_V + " " + VOLTAGE_UNIT));
    }

    @Test
    void testGetFeederInfosRespectsResultRespectsOrderWhenDirectionIsBottomAndFeederSymmetry() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(svgParametersMock.isFeederInfoSymmetry()).thenReturn(true);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getDirection()).thenReturn(Direction.BOTTOM);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(GENERATOR_WITH_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos.get(0).getRightLabel()).isEqualTo(Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(1).getRightLabel()).isEqualTo(Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(2).getRightLabel()).isEqualTo(Optional.of(TERMINAL_I + " " + CURRENT_UNIT));
        assertThat(actualFeederInfos.get(3).getRightLabel()).isEqualTo(Optional.of(MEASUREMENT_P + " " + ACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(4).getRightLabel()).isEqualTo(Optional.of(MEASUREMENT_Q + " " + REACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(5).getRightLabel()).isEqualTo(Optional.of(MEASUREMENT_V + " " + VOLTAGE_UNIT));
    }

    @Test
    void testGetFeederInfosResultRespectsOrderWhenDirectionIsBottom() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getDirection()).thenReturn(Direction.BOTTOM);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(GENERATOR_WITH_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos.get(0).getRightLabel()).isEqualTo(Optional.of(MEASUREMENT_V + " " + VOLTAGE_UNIT));
        assertThat(actualFeederInfos.get(1).getRightLabel()).isEqualTo(Optional.of(MEASUREMENT_Q + " " + REACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(2).getRightLabel()).isEqualTo(Optional.of(MEASUREMENT_P + " " + ACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(3).getRightLabel()).isEqualTo(Optional.of(TERMINAL_I + " " + CURRENT_UNIT));
        assertThat(actualFeederInfos.get(4).getRightLabel()).isEqualTo(Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT));
        assertThat(actualFeederInfos.get(5).getRightLabel()).isEqualTo(Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT));
    }

    @Test
    void testGetFeederInfosResultRespectsOrderWhenDirectionIsBottomAndNoMeasurements() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getDirection()).thenReturn(Direction.BOTTOM);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(GENERATOR_WITHOUT_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(3)
                .extracting("rightLabel")
                .containsExactly(
                        Optional.of(TERMINAL_I + " " + CURRENT_UNIT),
                        Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT),
                        Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT)
        );
    }

    @Test
    void testGetFeederInfosForBranchWithCurrentMeasurement() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.BRANCH);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn("BRANCH_WITH_CURRENT");

        Branch branchMock = Mockito.mock(Branch.class);
        Terminal terminalMock = Mockito.mock(Terminal.class);
        Mockito.lenient().when(branchMock.getTerminal(TwoSides.ONE)).thenReturn(terminalMock);
        Mockito.when(networkMock.getBranch("BRANCH_WITH_CURRENT")).thenReturn(branchMock);

        Measurement measurementMock = Mockito.mock(Measurement.class);
        Mockito.when(measurementMock.getType()).thenReturn(Measurement.Type.CURRENT);
        Mockito.when(measurementMock.getSide()).thenReturn(ThreeSides.ONE);
        Mockito.when(measurementMock.getValue()).thenReturn(100.0);
        Mockito.when(measurementMock.isValid()).thenReturn(true);

        Measurements measurementsMock = Mockito.mock(Measurements.class);
        Mockito.when(measurementsMock.getMeasurements()).thenReturn(List.of(measurementMock));
        Mockito.when(branchMock.getExtension(Measurements.class)).thenReturn(measurementsMock);

        BranchObservability observabilityMock = Mockito.mock(BranchObservability.class);
        Mockito.when(branchMock.getExtension(BranchObservability.class)).thenReturn(observabilityMock);

        Mockito.when(svgParametersMock.getUndefinedValueSymbol()).thenReturn("?");

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(ValueFeederInfo.class::isInstance)
                .extracting("rightLabel")
                .contains(Optional.of("100.0 ?"));
    }

    @Test
    void testGetFeederInfosForInjectionWithCurrentMeasurement() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn("GEN_WITH_CURRENT");

        Injection injectionMock = Mockito.mock(Injection.class);
        Terminal terminalMock = Mockito.mock(Terminal.class);
        Mockito.when(injectionMock.getTerminal()).thenReturn(terminalMock);
        Mockito.when(networkMock.getIdentifiable("GEN_WITH_CURRENT")).thenReturn(injectionMock);

        Measurement measurementMock = Mockito.mock(Measurement.class);
        Mockito.when(measurementMock.getType()).thenReturn(Measurement.Type.CURRENT);
        Mockito.when(measurementMock.getValue()).thenReturn(100.0);
        Mockito.when(measurementMock.isValid()).thenReturn(true);

        Measurements measurementsMock = Mockito.mock(Measurements.class);
        Mockito.when(measurementsMock.getMeasurements()).thenReturn(List.of(measurementMock));
        Mockito.when(injectionMock.getExtension(Measurements.class)).thenReturn(measurementsMock);

        InjectionObservability observabilityMock = Mockito.mock(InjectionObservability.class);
        Mockito.when(injectionMock.getExtension(InjectionObservability.class)).thenReturn(observabilityMock);

        Mockito.when(svgParametersMock.getUndefinedValueSymbol()).thenReturn("?");

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(ValueFeederInfo.class::isInstance)
                .extracting("rightLabel")
                .contains(Optional.of("100.0 ?"));
    }

    @Test
    void testGetFeederInfosMeasurementsSorting() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn("GEN_SORT");

        Injection injectionMock = Mockito.mock(Injection.class);
        Terminal terminalMock = Mockito.mock(Terminal.class);
        Mockito.when(injectionMock.getTerminal()).thenReturn(terminalMock);
        Mockito.when(networkMock.getIdentifiable("GEN_SORT")).thenReturn(injectionMock);

        Measurement m1 = Mockito.mock(Measurement.class);
        Mockito.when(m1.getType()).thenReturn(Measurement.Type.VOLTAGE);
        Mockito.when(m1.getValue()).thenReturn(15.0);
        Mockito.when(m1.isValid()).thenReturn(true);

        Measurement m2 = Mockito.mock(Measurement.class);
        Mockito.when(m2.getType()).thenReturn(Measurement.Type.ACTIVE_POWER);
        Mockito.when(m2.getValue()).thenReturn(13.0);
        Mockito.when(m2.isValid()).thenReturn(true);

        Measurements measurementsMock = Mockito.mock(Measurements.class);
        // Provide them in reverse order: VOLTAGE, then ACTIVE_POWER. They should be sorted to ACTIVE_POWER, then VOLTAGE.
        Mockito.when(measurementsMock.getMeasurements()).thenReturn(List.of(m1, m2));
        Mockito.when(injectionMock.getExtension(Measurements.class)).thenReturn(measurementsMock);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(fi -> fi instanceof ValueFeederInfo && fi.getUserDefinedId() != null)
                .extracting("rightLabel")
                .containsExactly(Optional.of("13.0 MW"), Optional.of("15.0 kV"));
    }

    @Test
    void testGetFeederInfosForBranchWithVoltageMeasurement() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.BRANCH);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn("BRANCH_WITH_VOLTAGE");

        Branch branchMock = Mockito.mock(Branch.class);
        Terminal terminalMock = Mockito.mock(Terminal.class);
        Mockito.lenient().when(branchMock.getTerminal(TwoSides.ONE)).thenReturn(terminalMock);
        Mockito.when(networkMock.getBranch("BRANCH_WITH_VOLTAGE")).thenReturn(branchMock);

        Measurement measurementMock = Mockito.mock(Measurement.class);
        Mockito.when(measurementMock.getType()).thenReturn(Measurement.Type.VOLTAGE);
        Mockito.when(measurementMock.getSide()).thenReturn(ThreeSides.ONE);
        Mockito.when(measurementMock.getValue()).thenReturn(400.0);
        Mockito.when(measurementMock.isValid()).thenReturn(true);

        Measurements measurementsMock = Mockito.mock(Measurements.class);
        Mockito.when(measurementsMock.getMeasurements()).thenReturn(List.of(measurementMock));
        Mockito.when(branchMock.getExtension(Measurements.class)).thenReturn(measurementsMock);

        BranchObservability observabilityMock = Mockito.mock(BranchObservability.class);
        Mockito.when(branchMock.getExtension(BranchObservability.class)).thenReturn(observabilityMock);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(fi -> fi instanceof ValueFeederInfo && fi.getUserDefinedId() != null)
                .extracting("rightLabel")
                .contains(Optional.of("400.0 kV"));
    }

    @Test
    void testGetFeederInfosForBranchWithVoltageMeasurementButNoBranchObservabilityQuality() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.BRANCH);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn("BRANCH_WITH_P_MEASUREMENT");

        Branch branchMock = Mockito.mock(Branch.class);
        Terminal terminalMock = Mockito.mock(Terminal.class);
        Mockito.lenient().when(branchMock.getTerminal(TwoSides.ONE)).thenReturn(terminalMock);
        Mockito.when(networkMock.getBranch("BRANCH_WITH_P_MEASUREMENT")).thenReturn(branchMock);

        Measurement measurementMock = Mockito.mock(Measurement.class);
        Mockito.when(measurementMock.getType()).thenReturn(Measurement.Type.ACTIVE_POWER);
        Mockito.when(measurementMock.getSide()).thenReturn(ThreeSides.ONE);
        Mockito.when(measurementMock.getValue()).thenReturn(400.0);
        Mockito.when(measurementMock.isValid()).thenReturn(true);

        Measurements measurementsMock = Mockito.mock(Measurements.class);
        Mockito.when(measurementsMock.getMeasurements()).thenReturn(List.of(measurementMock));
        Mockito.when(branchMock.getExtension(Measurements.class)).thenReturn(measurementsMock);

        BranchObservability observabilityMock = Mockito.mock(BranchObservability.class);
        Mockito.when(branchMock.getExtension(BranchObservability.class)).thenReturn(observabilityMock);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(fi -> fi instanceof ValueFeederInfo && fi.getUserDefinedId() != null)
                .extracting("rightLabel")
                .contains(Optional.of("400.0 MW"));
    }

    @Test
    void testGetFeederInfosWhenDirectionIsNull() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getDirection()).thenReturn(null);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(GENERATOR_WITHOUT_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(3);
        assertThat(actualFeederInfos.getFirst().getRightLabel()).isEqualTo(Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT));
    }

    @Test
    void testGetFeederInfosForHvdcWithNodeSideTwoNoMeasurements() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.HVDC);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.TWO);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(HVDC_WITHOUT_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(3);
    }

    @Test
    void testGetFeederInfosBranchMeasurementsSorting() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.BRANCH);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn("BRANCH_SORT");

        Branch branchMock = Mockito.mock(Branch.class);
        Terminal terminalMock = Mockito.mock(Terminal.class);
        Mockito.lenient().when(branchMock.getTerminal(TwoSides.ONE)).thenReturn(terminalMock);
        Mockito.when(networkMock.getBranch("BRANCH_SORT")).thenReturn(branchMock);

        Measurement m1 = Mockito.mock(Measurement.class);
        Mockito.when(m1.getType()).thenReturn(Measurement.Type.REACTIVE_POWER);
        Mockito.when(m1.getSide()).thenReturn(ThreeSides.ONE);
        Mockito.when(m1.getValue()).thenReturn(11.0);
        Mockito.when(m1.isValid()).thenReturn(true);

        Measurement m2 = Mockito.mock(Measurement.class);
        Mockito.when(m2.getType()).thenReturn(Measurement.Type.ACTIVE_POWER);
        Mockito.when(m2.getSide()).thenReturn(ThreeSides.ONE);
        Mockito.when(m2.getValue()).thenReturn(10.0);
        Mockito.when(m2.isValid()).thenReturn(true);

        Measurements measurementsMock = Mockito.mock(Measurements.class);
        Mockito.when(measurementsMock.getMeasurements()).thenReturn(List.of(m1, m2));
        Mockito.when(branchMock.getExtension(Measurements.class)).thenReturn(measurementsMock);

        BranchObservability observabilityMock = Mockito.mock(BranchObservability.class);
        Mockito.when(branchMock.getExtension(BranchObservability.class)).thenReturn(observabilityMock);

        ObservabilityQuality qualityMock = Mockito.mock(ObservabilityQuality.class);
        Mockito.when(qualityMock.isRedundant()).thenReturn(Optional.of(true));
        Mockito.when(observabilityMock.getQualityP1()).thenReturn(qualityMock);
        Mockito.when(observabilityMock.getQualityQ1()).thenReturn(qualityMock);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(fi -> fi instanceof ValueFeederInfo && fi.getUserDefinedId() != null)
                .extracting("rightLabel")
                .containsExactly(Optional.of("10.0 MW"), Optional.of("11.0 Mvar"));
    }

    private void prepareMocks() {
        ValueFormatter valueFormatter = new ValueFormatter(1, 1, 1, 1, 1, Locale.US, "?");
        Mockito.lenient().when(svgParametersMock.createValueFormatter()).thenReturn(valueFormatter);
        Mockito.lenient().when(svgParametersMock.getCurrentUnit()).thenReturn(CURRENT_UNIT);
        Mockito.lenient().when(svgParametersMock.getActivePowerUnit()).thenReturn(ACTIVE_POWER_UNIT);
        Mockito.lenient().when(svgParametersMock.getReactivePowerUnit()).thenReturn(REACTIVE_POWER_UNIT);

        Injection<?> injectionNoMeasurementsMock = Mockito.mock(Injection.class);
        Injection<?> injectionWithMeasurementsMock = Mockito.mock(Injection.class);
        Injection<?> injectionWithMeasurementsNoObservabilityMock = Mockito.mock(Injection.class);
        Branch<?> branchNoMeasurementsMock = Mockito.mock(Branch.class);
        Branch<?> branchWithMeasurementsMock = Mockito.mock(Branch.class);
        HvdcLine hvdcNoMeasurementsMock = Mockito.mock(HvdcLine.class);
        HvdcLine hvdcWithMeasurementsMock = Mockito.mock(HvdcLine.class);

        Terminal terminalMock = Mockito.mock(Terminal.class);
        Mockito.lenient().when(terminalMock.getP()).thenReturn(TERMINAL_P);
        Mockito.lenient().when(terminalMock.getQ()).thenReturn(TERMINAL_Q);
        Mockito.lenient().when(terminalMock.getI()).thenReturn(TERMINAL_I);

        prepareInjectionMocks(injectionNoMeasurementsMock, injectionWithMeasurementsMock, injectionWithMeasurementsNoObservabilityMock, terminalMock);

        prepareBranchMocks(branchNoMeasurementsMock, branchWithMeasurementsMock, terminalMock);

        prepareHvdcMocks(hvdcNoMeasurementsMock, hvdcWithMeasurementsMock, terminalMock);

        Mockito.lenient().when(networkMock.getIdentifiable(GENERATOR_WITHOUT_MEASUREMENTS)).thenReturn((Identifiable) injectionNoMeasurementsMock);
        Mockito.lenient().when(networkMock.getIdentifiable(GENERATOR_WITH_MEASUREMENTS)).thenReturn((Identifiable) injectionWithMeasurementsMock);
        Mockito.lenient().when(networkMock.getIdentifiable(GENERATOR_WITH_MEASUREMENTS_NO_OBSERVABILITY)).thenReturn((Identifiable) injectionWithMeasurementsNoObservabilityMock);
        Mockito.lenient().when(networkMock.getBranch(BRANCH_WITHOUT_MEASUREMENTS)).thenReturn(branchNoMeasurementsMock);
        Mockito.lenient().when(networkMock.getBranch(BRANCH_WITH_MEASUREMENTS)).thenReturn(branchWithMeasurementsMock);
        Mockito.lenient().when(networkMock.getHvdcLine(HVDC_WITHOUT_MEASUREMENTS)).thenReturn(hvdcNoMeasurementsMock);
        Mockito.lenient().when(networkMock.getHvdcLine(HVDC_WITH_MEASUREMENTS)).thenReturn(hvdcWithMeasurementsMock);
    }

    private void prepareHvdcMocks(HvdcLine hvdcLineNoMeasurements, HvdcLine hvdcLineWithMeasurements, Terminal terminalMock) {
        Measurement measurementPSideOneMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementPSideOneMock.getSide()).thenReturn(ThreeSides.ONE);
        Mockito.lenient().when(measurementPSideOneMock.getType()).thenReturn(Measurement.Type.ACTIVE_POWER);
        Mockito.lenient().when(measurementPSideOneMock.getValue()).thenReturn(MEASUREMENT_P1);
        Mockito.lenient().when(measurementPSideOneMock.isValid()).thenReturn(false);
        Measurement measurementPSideTwoMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementPSideTwoMock.getSide()).thenReturn(ThreeSides.TWO);
        Mockito.lenient().when(measurementPSideTwoMock.getType()).thenReturn(Measurement.Type.ACTIVE_POWER);
        Mockito.lenient().when(measurementPSideTwoMock.getValue()).thenReturn(MEASUREMENT_P2);
        Mockito.lenient().when(measurementPSideTwoMock.isValid()).thenReturn(true);
        Measurement measurementQSideOneMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementQSideOneMock.getSide()).thenReturn(ThreeSides.ONE);
        Mockito.lenient().when(measurementQSideOneMock.getType()).thenReturn(Measurement.Type.REACTIVE_POWER);
        Mockito.lenient().when(measurementQSideOneMock.getValue()).thenReturn(MEASUREMENT_Q1);
        Mockito.lenient().when(measurementQSideOneMock.isValid()).thenReturn(false);
        Measurement measurementQSideTwoMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementQSideTwoMock.getSide()).thenReturn(ThreeSides.TWO);
        Mockito.lenient().when(measurementQSideTwoMock.getType()).thenReturn(Measurement.Type.REACTIVE_POWER);
        Mockito.lenient().when(measurementQSideTwoMock.getValue()).thenReturn(MEASUREMENT_Q2);
        Mockito.lenient().when(measurementQSideTwoMock.isValid()).thenReturn(true);
        Measurements<?> measurementsSideOneMock = Mockito.mock(Measurements.class);
        Measurements<?> measurementsSideTwoMock = Mockito.mock(Measurements.class);
        Mockito.lenient().when(measurementsSideOneMock.getMeasurements()).thenReturn(List.of(measurementPSideOneMock, measurementQSideOneMock));
        Mockito.lenient().when(measurementsSideTwoMock.getMeasurements()).thenReturn(List.of(measurementPSideTwoMock, measurementQSideTwoMock));

        ObservabilityQuality<?> redundantObservabilityQuality = Mockito.mock(ObservabilityQuality.class);
        Mockito.lenient().when(redundantObservabilityQuality.isRedundant()).thenReturn(Optional.of(true));
        ObservabilityQuality<?> nonRedundantObservabilityQuality = Mockito.mock(ObservabilityQuality.class);
        Mockito.lenient().when(nonRedundantObservabilityQuality.isRedundant()).thenReturn(Optional.of(false));
        InjectionObservability<?> injectionObservabilitySideOneMock = Mockito.mock(InjectionObservability.class);
        Mockito.lenient().when(injectionObservabilitySideOneMock.getQualityP()).thenReturn((ObservabilityQuality) redundantObservabilityQuality);
        Mockito.lenient().when(injectionObservabilitySideOneMock.getQualityQ()).thenReturn((ObservabilityQuality) redundantObservabilityQuality);
        InjectionObservability<?> injectionObservabilitySideTwoMock = Mockito.mock(InjectionObservability.class);
        Mockito.lenient().when(injectionObservabilitySideTwoMock.getQualityP()).thenReturn((ObservabilityQuality) nonRedundantObservabilityQuality);
        Mockito.lenient().when(injectionObservabilitySideTwoMock.getQualityQ()).thenReturn((ObservabilityQuality) nonRedundantObservabilityQuality);

        HvdcConverterStation<?> hvdcConverterStationWithoutMeasurementsOne = Mockito.mock(HvdcConverterStation.class);
        Mockito.lenient().when(hvdcConverterStationWithoutMeasurementsOne.getTerminal()).thenReturn(terminalMock);

        HvdcConverterStation<?> hvdcConverterStationWithoutMeasurementsTwo = Mockito.mock(HvdcConverterStation.class);
        Mockito.lenient().when(hvdcConverterStationWithoutMeasurementsTwo.getTerminal()).thenReturn(terminalMock);

        HvdcConverterStation<?> hvdcConverterStationWithMeasurementsOne = Mockito.mock(HvdcConverterStation.class);
        Mockito.lenient().when(hvdcConverterStationWithMeasurementsOne.getTerminal()).thenReturn(terminalMock);
        Mockito.lenient().when(hvdcConverterStationWithMeasurementsOne.getExtension(Measurements.class)).thenReturn(measurementsSideOneMock);
        Mockito.lenient().when(hvdcConverterStationWithMeasurementsOne.getExtension(InjectionObservability.class)).thenReturn(injectionObservabilitySideOneMock);

        HvdcConverterStation<?> hvdcConverterStationWithMeasurementsTwo = Mockito.mock(HvdcConverterStation.class);
        Mockito.lenient().when(hvdcConverterStationWithMeasurementsTwo.getTerminal()).thenReturn(terminalMock);
        Mockito.lenient().when(hvdcConverterStationWithMeasurementsTwo.getExtension(Measurements.class)).thenReturn(measurementsSideTwoMock);
        Mockito.lenient().when(hvdcConverterStationWithMeasurementsTwo.getExtension(InjectionObservability.class)).thenReturn(injectionObservabilitySideTwoMock);

        Mockito.lenient().when(hvdcLineNoMeasurements.getConverterStation1()).thenReturn((HvdcConverterStation) hvdcConverterStationWithoutMeasurementsOne);
        Mockito.lenient().when(hvdcLineNoMeasurements.getConverterStation2()).thenReturn((HvdcConverterStation) hvdcConverterStationWithoutMeasurementsTwo);
        Mockito.lenient().when(hvdcLineWithMeasurements.getConverterStation1()).thenReturn((HvdcConverterStation) hvdcConverterStationWithMeasurementsOne);
        Mockito.lenient().when(hvdcLineWithMeasurements.getConverterStation2()).thenReturn((HvdcConverterStation) hvdcConverterStationWithMeasurementsTwo);
    }

    private void prepareBranchMocks(Branch<?> branchNoMeasurements, Branch<?> branchWithMeasurements, Terminal terminalMock) {
        Measurement measurementPSideOneMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementPSideOneMock.getSide()).thenReturn(ThreeSides.ONE);
        Mockito.lenient().when(measurementPSideOneMock.getType()).thenReturn(Measurement.Type.ACTIVE_POWER);
        Mockito.lenient().when(measurementPSideOneMock.getValue()).thenReturn(MEASUREMENT_P1);
        Mockito.lenient().when(measurementPSideOneMock.isValid()).thenReturn(false);
        Measurement measurementPSideTwoMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementPSideTwoMock.getSide()).thenReturn(ThreeSides.TWO);
        Mockito.lenient().when(measurementPSideTwoMock.getType()).thenReturn(Measurement.Type.ACTIVE_POWER);
        Mockito.lenient().when(measurementPSideTwoMock.getValue()).thenReturn(MEASUREMENT_P2);
        Mockito.lenient().when(measurementPSideTwoMock.isValid()).thenReturn(true);
        Measurement measurementQSideOneMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementQSideOneMock.getSide()).thenReturn(ThreeSides.ONE);
        Mockito.lenient().when(measurementQSideOneMock.getType()).thenReturn(Measurement.Type.REACTIVE_POWER);
        Mockito.lenient().when(measurementQSideOneMock.getValue()).thenReturn(MEASUREMENT_Q1);
        Mockito.lenient().when(measurementQSideOneMock.isValid()).thenReturn(false);
        Measurement measurementQSideTwoMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementQSideTwoMock.getSide()).thenReturn(ThreeSides.TWO);
        Mockito.lenient().when(measurementQSideTwoMock.getType()).thenReturn(Measurement.Type.REACTIVE_POWER);
        Mockito.lenient().when(measurementQSideTwoMock.getValue()).thenReturn(MEASUREMENT_Q2);
        Mockito.lenient().when(measurementQSideTwoMock.isValid()).thenReturn(true);
        Measurements<?> measurementsMock = Mockito.mock(Measurements.class);
        Mockito.lenient().when(measurementsMock.getMeasurements()).thenReturn(List.of(measurementPSideOneMock, measurementPSideTwoMock, measurementQSideOneMock, measurementQSideTwoMock));

        ObservabilityQuality<?> redundantObservabilityQuality = Mockito.mock(ObservabilityQuality.class);
        Mockito.lenient().when(redundantObservabilityQuality.isRedundant()).thenReturn(Optional.of(true));
        ObservabilityQuality<?> nonRedundantObservabilityQuality = Mockito.mock(ObservabilityQuality.class);
        Mockito.lenient().when(nonRedundantObservabilityQuality.isRedundant()).thenReturn(Optional.of(false));
        BranchObservability<?> branchObservabilityMock = Mockito.mock(BranchObservability.class);
        Mockito.lenient().when(branchObservabilityMock.getQualityP1()).thenReturn((ObservabilityQuality) redundantObservabilityQuality);
        Mockito.lenient().when(branchObservabilityMock.getQualityQ1()).thenReturn((ObservabilityQuality) redundantObservabilityQuality);
        Mockito.lenient().when(branchObservabilityMock.getQualityP2()).thenReturn((ObservabilityQuality) nonRedundantObservabilityQuality);
        Mockito.lenient().when(branchObservabilityMock.getQualityQ2()).thenReturn((ObservabilityQuality) nonRedundantObservabilityQuality);

        Mockito.lenient().when(branchNoMeasurements.getTerminal(TwoSides.ONE)).thenReturn(terminalMock);
        Mockito.lenient().when(branchNoMeasurements.getTerminal(TwoSides.TWO)).thenReturn(terminalMock);
        Mockito.lenient().when(branchWithMeasurements.getTerminal(TwoSides.ONE)).thenReturn(terminalMock);
        Mockito.lenient().when(branchWithMeasurements.getTerminal(TwoSides.TWO)).thenReturn(terminalMock);

        Mockito.lenient().when(branchWithMeasurements.getExtension(Measurements.class)).thenReturn(measurementsMock);
        Mockito.lenient().when(branchWithMeasurements.getExtension(BranchObservability.class)).thenReturn(branchObservabilityMock);
    }

    private void prepareInjectionMocks(Injection<?> injectionNoMeasurementsMock, Injection<?> injectionWithMeasurementsMock, Injection<?> injectionWithMeasurementsNoObservabilityMock, Terminal terminalMock) {
        Measurement measurementPMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementPMock.getType()).thenReturn(Measurement.Type.ACTIVE_POWER);
        Mockito.lenient().when(measurementPMock.getValue()).thenReturn(MEASUREMENT_P);
        Mockito.lenient().when(measurementPMock.isValid()).thenReturn(true);
        Measurement measurementQMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementQMock.getType()).thenReturn(Measurement.Type.REACTIVE_POWER);
        Mockito.lenient().when(measurementQMock.getValue()).thenReturn(MEASUREMENT_Q);
        Mockito.lenient().when(measurementQMock.isValid()).thenReturn(false);
        Measurement measurementVMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementVMock.getType()).thenReturn(Measurement.Type.VOLTAGE);
        Mockito.lenient().when(measurementVMock.getValue()).thenReturn(MEASUREMENT_V);
        Mockito.lenient().when(measurementVMock.isValid()).thenReturn(false);
        Measurements<?> measurementsMock = Mockito.mock(Measurements.class);
        Mockito.lenient().when(measurementsMock.getMeasurements()).thenReturn(List.of(measurementPMock, measurementQMock, measurementVMock));

        ObservabilityQuality<?> redundantObservabilityQuality = Mockito.mock(ObservabilityQuality.class);
        Mockito.lenient().when(redundantObservabilityQuality.isRedundant()).thenReturn(Optional.of(true));
        ObservabilityQuality<?> nonRedundantObservabilityQuality = Mockito.mock(ObservabilityQuality.class);
        Mockito.lenient().when(nonRedundantObservabilityQuality.isRedundant()).thenReturn(Optional.of(false));
        ObservabilityQuality<?> missingRedundantObservabilityQuality = Mockito.mock(ObservabilityQuality.class);
        Mockito.lenient().when(missingRedundantObservabilityQuality.isRedundant()).thenReturn(Optional.empty());
        InjectionObservability<?> injectionObservabilityMock = Mockito.mock(InjectionObservability.class);
        Mockito.lenient().when(injectionObservabilityMock.getQualityP()).thenReturn((ObservabilityQuality) redundantObservabilityQuality);
        Mockito.lenient().when(injectionObservabilityMock.getQualityQ()).thenReturn((ObservabilityQuality) nonRedundantObservabilityQuality);
        Mockito.lenient().when(injectionObservabilityMock.getQualityV()).thenReturn((ObservabilityQuality) missingRedundantObservabilityQuality);

        Mockito.lenient().when(injectionNoMeasurementsMock.getTerminal()).thenReturn(terminalMock);
        Mockito.lenient().when(injectionWithMeasurementsMock.getTerminal()).thenReturn(terminalMock);
        Mockito.lenient().when(injectionWithMeasurementsNoObservabilityMock.getTerminal()).thenReturn(terminalMock);
        Mockito.lenient().when(injectionWithMeasurementsMock.getExtension(Measurements.class)).thenReturn(measurementsMock);
        Mockito.lenient().when(injectionWithMeasurementsNoObservabilityMock.getExtension(Measurements.class)).thenReturn(measurementsMock);
        Mockito.lenient().when(injectionWithMeasurementsMock.getExtension(InjectionObservability.class)).thenReturn(injectionObservabilityMock);
    }
}
