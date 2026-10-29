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
import com.powsybl.sld.model.graphs.VoltageLevelInfos;
import com.powsybl.sld.model.nodes.BusNode;
import com.powsybl.sld.model.nodes.Feeder;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.model.nodes.FeederType;
import com.powsybl.sld.model.nodes.NodeSide;
import com.powsybl.sld.model.nodes.feeders.FeederTeePointLeg;
import com.powsybl.sld.model.nodes.feeders.FeederTwLeg;
import com.powsybl.sld.model.nodes.feeders.FeederWithSides;
import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.LabelProvider;
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
class MeasurementLabelProviderTest {

    private static final String REACTIVE_POWER_UNIT = "Mvar";
    private static final String ACTIVE_POWER_UNIT = "MW";
    private static final String CURRENT_UNIT = "A";
    private static final String GENERATOR_WITHOUT_MEASUREMENTS = "GEN_NO_MEASUREMENTS";
    private static final String GENERATOR_WITH_MEASUREMENTS = "GEN_WITH_MEASUREMENTS";
    private static final String GENERATOR_WITH_MEASUREMENTS_NO_OBSERVABILITY = "GEN_WITH_MEASUREMENTS_NO_OBSERVABILITY";
    private static final String BRANCH_WITHOUT_MEASUREMENTS = "BRANCH_NO_MEASUREMENTS";
    private static final String BRANCH_WITH_MEASUREMENTS = "BRANCH_WITH_MEASUREMENTS";
    private static final String TWO_WINDINGS_TRANSFORMER_LEG_WITHOUT_MEASUREMENTS = "TWO_WINDINGS_TRANSFORMER_LEG_NO_MEASUREMENTS";
    private static final String TWO_WINDINGS_TRANSFORMER_LEG_WITH_MEASUREMENTS = "TWO_WINDINGS_TRANSFORMER_LEG_WITH_MEASUREMENTS";
    private static final String HVDC_WITHOUT_MEASUREMENTS = "HVDC_NO_MEASUREMENTS";
    private static final String HVDC_WITH_MEASUREMENTS = "HVDC_WITH_MEASUREMENTS";
    private static final String BATTERY_WITHOUT_MEASUREMENTS = "BATTERY_NO_MEASUREMENTS";
    private static final String CAPACITOR_WITHOUT_MEASUREMENTS = "CAPACITOR_NO_MEASUREMENTS";
    private static final String INDUCTOR_WITHOUT_MEASUREMENTS = "INDUCTOR_NO_MEASUREMENTS";
    private static final String SVC_WITHOUT_MEASUREMENTS = "SVC_NO_MEASUREMENTS";
    private static final String LOAD_WITHOUT_MEASUREMENTS = "LOAD_NO_MEASUREMENTS";
    private static final String VSC_WITHOUT_MEASUREMENTS = "VSC_NO_MEASUREMENTS";
    private static final String GENERATOR_WITH_PARTIAL_MEASUREMENTS = "GEN_PARTIAL_MEASUREMENTS";
    private static final String GENERATOR_WITH_EXTRA_MEASUREMENTS = "GEN_EXTRA_MEASUREMENTS";
    private static final String GENERATOR_WITH_NAN_MEASUREMENT = "GEN_NAN_MEASUREMENT";
    private static final String LINE_WITH_PARTIAL_MEASUREMENTS = "LINE_PARTIAL_MEASUREMENTS";
    private static final String TRANSFORMER_WITH_PARTIAL_MEASUREMENTS = "TRANSFORMER_PARTIAL_MEASUREMENTS";
    private static final String HVDC_WITH_PARTIAL_MEASUREMENTS = "HVDC_PARTIAL_MEASUREMENTS";
    private static final String THREE_WINDINGS_TRANSFORMER_WITH_MEASUREMENTS = "THREE_WINDINGS_TRANSFORMER_WITH_MEASUREMENTS";
    private static final String TEE_POINT_LINE_WITH_MEASUREMENTS = "TEE_POINT_LINE_WITH_MEASUREMENTS";
    private static final double TERMINAL_P = 10D;
    private static final double TERMINAL_Q = 11D;
    private static final double TERMINAL_I = 12D;
    private static final double MEASUREMENT_P = 13D;
    private static final double MEASUREMENT_Q = 14D;
    private static final double MEASUREMENT_P1 = 16D;
    private static final double MEASUREMENT_Q1 = 17D;
    private static final double MEASUREMENT_P2 = 18D;
    private static final double MEASUREMENT_Q2 = 19D;
    private static final double MEASUREMENT_V = 225.123D;
    private static final String UNDEFINED_VALUE_SYMBOL = "?";

    @Mock
    private Network networkMock;
    @Mock
    private SldComponentLibrary sldComponentLibraryMock;
    @Mock
    private LayoutParameters layoutParametersMock;
    @Mock
    private SvgParameters svgParametersMock;

    private MeasurementLabelProvider provider;

    private static Stream<Arguments> provideSidedMeasurements() {
        return Stream.of(
                Arguments.of(NodeSide.ONE, MEASUREMENT_P1, MEASUREMENT_Q1, false, false),
                Arguments.of(NodeSide.TWO, MEASUREMENT_P2, MEASUREMENT_Q2, true, true)
        );
    }

    private static Stream<Arguments> provideInjectionFamilies() {
        return Stream.of(
                Arguments.of(Generator.class, GENERATOR_WITHOUT_MEASUREMENTS, List.of("? MW", "? Mvar", "?")),
                Arguments.of(Battery.class, BATTERY_WITHOUT_MEASUREMENTS, List.of("? MW", "?")),
                Arguments.of(ShuntCompensator.class, CAPACITOR_WITHOUT_MEASUREMENTS, List.of("? Mvar", "?")),
                Arguments.of(ShuntCompensator.class, INDUCTOR_WITHOUT_MEASUREMENTS, List.of("? Mvar", "?")),
                Arguments.of(StaticVarCompensator.class, SVC_WITHOUT_MEASUREMENTS, List.of("? Mvar", "?")),
                Arguments.of(Load.class, LOAD_WITHOUT_MEASUREMENTS, List.of("? MW", "? Mvar", "?")),
                Arguments.of(VscConverterStation.class, VSC_WITHOUT_MEASUREMENTS, List.of("? MW", "? Mvar", "?"))
        ).flatMap(arguments -> Stream.of(false, true).map(emptyExtension -> Arguments.of(
                arguments.get()[0], arguments.get()[1], arguments.get()[2], emptyExtension)));
    }

    private static Stream<Arguments> provideSidedEquipment() {
        return Stream.of(
                Arguments.of(Line.class, LINE_WITH_PARTIAL_MEASUREMENTS, FeederType.BRANCH),
                Arguments.of(TwoWindingsTransformer.class, TRANSFORMER_WITH_PARTIAL_MEASUREMENTS, FeederType.BRANCH),
                Arguments.of(TwoWindingsTransformer.class, TRANSFORMER_WITH_PARTIAL_MEASUREMENTS, FeederType.TWO_WINDINGS_TRANSFORMER_LEG)
        ).flatMap(arguments -> Stream.of(NodeSide.ONE, NodeSide.TWO).map(side -> Arguments.of(
                arguments.get()[0], arguments.get()[1], arguments.get()[2], side)));
    }

    private static Stream<Arguments> provideMissingSidedMeasurements() {
        return provideSidedEquipment().flatMap(arguments -> Stream.of(false, true).map(emptyExtension -> Arguments.of(
                arguments.get()[0], arguments.get()[1], arguments.get()[2], arguments.get()[3], emptyExtension)));
    }

    private static Stream<Arguments> providePartialInjectionMeasurements() {
        return Stream.of(
                Arguments.of(Generator.class, Measurement.Type.ACTIVE_POWER, List.of("13.0", "?", "?")),
                Arguments.of(Generator.class, Measurement.Type.REACTIVE_POWER, List.of("?", "13.0", "?")),
                Arguments.of(Battery.class, Measurement.Type.ACTIVE_POWER, List.of("13.0", "?")),
                Arguments.of(ShuntCompensator.class, Measurement.Type.REACTIVE_POWER, List.of("13.0", "?")),
                Arguments.of(StaticVarCompensator.class, Measurement.Type.VOLTAGE, List.of("?", "13.0"))
        );
    }

    private static Stream<Arguments> provideMeasurementOrdering() {
        return Stream.of(
                Arguments.of(Direction.TOP, false, List.of("10.0", "11.0", "12.0", "13.0", "?", "225.1")),
                Arguments.of(Direction.BOTTOM, true, List.of("10.0", "11.0", "12.0", "13.0", "?", "225.1")),
                Arguments.of(Direction.BOTTOM, false, List.of("225.1", "?", "13.0", "12.0", "11.0", "10.0")),
                Arguments.of(null, false, List.of("10.0", "11.0", "12.0", "13.0", "?", "225.1"))
        );
    }

    private static Stream<Arguments> provideBranchMeasurementOrdering() {
        return Stream.of(
                Arguments.of(Direction.TOP, false, List.of("10.0", "11.0", "16.0", "?", "225.1")),
                Arguments.of(Direction.BOTTOM, true, List.of("10.0", "11.0", "16.0", "?", "225.1")),
                Arguments.of(Direction.BOTTOM, false, List.of("225.1", "?", "16.0", "11.0", "10.0"))
        );
    }

    private static Stream<Arguments> provideExcludedEquipment() {
        return Stream.of(
                Arguments.of(BoundaryLine.class, "BOUNDARY_LINE", FeederType.INJECTION),
                Arguments.of(LccConverterStation.class, "LCC_INJECTION", FeederType.INJECTION),
                Arguments.of(Injection.class, "GENERIC_INJECTION", FeederType.INJECTION),
                Arguments.of(TieLine.class, "TIE_LINE", FeederType.BRANCH),
                Arguments.of(Branch.class, "GENERIC_BRANCH", FeederType.BRANCH)
        );
    }

    @BeforeEach
    void setUp() {
        prepareMocks();
        provider = new MeasurementLabelProvider(networkMock, sldComponentLibraryMock, layoutParametersMock, svgParametersMock);
    }

    @Test
    void testGetFeederInfosReturnsBaseClassResultAndMissingMeasurementsForInjectionWithoutMeasurements() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        Feeder feederMock = Mockito.mock(Feeder.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.INJECTION);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(GENERATOR_WITHOUT_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(6)
                .filteredOn(feederInfo -> !(feederInfo instanceof MeasurementFeederInfo))
                .hasSize(3)
                .allSatisfy(feederInfo -> assertThat(feederInfo).isExactlyInstanceOf(ValueFeederInfo.class))
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
            tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
            tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
            tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null)
        );
        assertMissingMeasurements(actualFeederInfos, "? MW", "? Mvar", "?");
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
                .filteredOn(feederInfo -> feederInfo instanceof ValueFeederInfo && !(feederInfo instanceof MeasurementFeederInfo))
                .hasSize(3)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
            tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
            tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
            tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null)
        );
        assertThat(actualFeederInfos).filteredOn(MeasurementFeederInfo.class::isInstance)
                .hasSize(3)
                .extracting("componentType", "rightLabel", "isValid", "isCritical", "isMissing")
                .containsExactly(
            tuple("VALUE_CURRENT", Optional.of(MEASUREMENT_P + " " + ACTIVE_POWER_UNIT), true, false, false),
            tuple("VALUE_CURRENT", Optional.of(MEASUREMENT_Q + " " + REACTIVE_POWER_UNIT), false, true, false),
            tuple("VALUE_CURRENT", Optional.of(UNDEFINED_VALUE_SYMBOL), false, false, true)
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
                .filteredOn(feederInfo -> feederInfo instanceof ValueFeederInfo && !(feederInfo instanceof MeasurementFeederInfo))
                .hasSize(3)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
                        tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
                        tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
                        tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null)
        );
        assertThat(actualFeederInfos).filteredOn(MeasurementFeederInfo.class::isInstance)
                .hasSize(3)
                .extracting("componentType", "rightLabel", "isValid", "isCritical", "isMissing")
                .containsExactly(
                        tuple("VALUE_CURRENT", Optional.of(MEASUREMENT_P + " " + ACTIVE_POWER_UNIT), true, false, false),
                        tuple("VALUE_CURRENT", Optional.of(MEASUREMENT_Q + " " + REACTIVE_POWER_UNIT), false, false, false),
                        tuple("VALUE_CURRENT", Optional.of(UNDEFINED_VALUE_SYMBOL), false, false, true)
        );
    }

    @Test
    void testGetFeederInfosReturnsFilteredBaseClassResultAndMissingMeasurementsForBranchWithNoMeasurements() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.BRANCH);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(BRANCH_WITHOUT_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(5)
                .filteredOn(feederInfo -> !(feederInfo instanceof MeasurementFeederInfo))
                .hasSize(2)
                .allSatisfy(feederInfo -> assertThat(feederInfo).isExactlyInstanceOf(ValueFeederInfo.class))
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
                        tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
                        tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null)
        );
        assertMissingMeasurements(actualFeederInfos, "? MW", "? Mvar", "?");
    }

    @ParameterizedTest
    @MethodSource("provideSidedMeasurements")
    void testGetFeederInfosReturnsBaseClassResultAndSidedMeasurementsForBranchWithMeasurements(NodeSide nodeSide, double measurementP, double measurementQ, boolean isValid, boolean isCritical) {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.BRANCH);
        Mockito.when(feederMock.getSide()).thenReturn(nodeSide);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(BRANCH_WITH_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(5)
                .filteredOn(feederInfo -> feederInfo instanceof ValueFeederInfo && !(feederInfo instanceof MeasurementFeederInfo))
                .hasSize(2)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
                        tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
                        tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null)
        );
        assertThat(actualFeederInfos).filteredOn(MeasurementFeederInfo.class::isInstance)
                .hasSize(3)
                .extracting("componentType", "rightLabel", "isValid", "isCritical", "isMissing")
                .containsExactly(
                        tuple("VALUE_CURRENT", Optional.of(measurementP + " " + ACTIVE_POWER_UNIT), isValid, isCritical, false),
                        tuple("VALUE_CURRENT", Optional.of(measurementQ + " " + REACTIVE_POWER_UNIT), isValid, isCritical, false),
                        tuple("VALUE_CURRENT", Optional.of(UNDEFINED_VALUE_SYMBOL), false, false, true)
                );
    }

    @Test
    void testGetFeederInfosReturnsBaseClassResultAndMissingMeasurementsForHvdcWithNoMeasurements() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.HVDC);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(HVDC_WITHOUT_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(6)
                .filteredOn(feederInfo -> !(feederInfo instanceof MeasurementFeederInfo))
                .hasSize(3)
                .allSatisfy(feederInfo -> assertThat(feederInfo).isExactlyInstanceOf(ValueFeederInfo.class))
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
                tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
                tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
                tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null)
        );
        assertMissingMeasurements(actualFeederInfos, "? MW", "? Mvar", "?");
    }

    @ParameterizedTest
    @MethodSource("provideSidedMeasurements")
    void testGetFeederInfosReturnsBaseClassResultAndSidedMeasurementsForHvdcWithMeasurements(NodeSide nodeSide, double measurementP, double measurementQ, boolean isValid, boolean isCritical) {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.HVDC);
        Mockito.when(feederMock.getSide()).thenReturn(nodeSide);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(HVDC_WITH_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(6)
                .filteredOn(feederInfo -> feederInfo instanceof ValueFeederInfo && !(feederInfo instanceof MeasurementFeederInfo))
                .hasSize(3)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
            tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
            tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null),
            tuple("VALUE_CURRENT", Optional.of(TERMINAL_I + " " + CURRENT_UNIT), null)
        );
        assertThat(actualFeederInfos).filteredOn(MeasurementFeederInfo.class::isInstance)
                .hasSize(3)
                .extracting("componentType", "rightLabel", "isValid", "isCritical", "isMissing")
                .containsExactly(
            tuple("VALUE_CURRENT", Optional.of(measurementP + " " + ACTIVE_POWER_UNIT), isValid, isCritical, false),
            tuple("VALUE_CURRENT", Optional.of(measurementQ + " " + REACTIVE_POWER_UNIT), isValid, isCritical, false),
            tuple("VALUE_CURRENT", Optional.of(UNDEFINED_VALUE_SYMBOL), false, false, true)
        );
    }

    @Test
    void testGetFeederInfosReturnsFilteredBaseClassResultAndMissingMeasurementsForTwoWindingTransformerLegWithNoMeasurements() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederTwLeg feederMock = Mockito.mock(FeederTwLeg.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.TWO_WINDINGS_TRANSFORMER_LEG);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(TWO_WINDINGS_TRANSFORMER_LEG_WITHOUT_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(5)
                .filteredOn(feederInfo -> !(feederInfo instanceof MeasurementFeederInfo))
                .hasSize(2)
                .allSatisfy(feederInfo -> assertThat(feederInfo).isExactlyInstanceOf(ValueFeederInfo.class))
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
                        tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
                        tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null)
        );
        assertMissingMeasurements(actualFeederInfos, "? MW", "? Mvar", "?");
    }

    @ParameterizedTest
    @MethodSource("provideSidedMeasurements")
    void testGetFeederInfosReturnsBaseClassResultAndSidedMeasurementsForTwoWindingTransformerLegWithMeasurements(NodeSide nodeSide, double measurementP, double measurementQ,
                                                                                                                 boolean isValid, boolean isCritical) {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederTwLeg feederMock = Mockito.mock(FeederTwLeg.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.TWO_WINDINGS_TRANSFORMER_LEG);
        Mockito.when(feederMock.getSide()).thenReturn(nodeSide);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn(TWO_WINDINGS_TRANSFORMER_LEG_WITH_MEASUREMENTS);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).hasSize(5)
                .filteredOn(feederInfo -> feederInfo instanceof ValueFeederInfo && !(feederInfo instanceof MeasurementFeederInfo))
                .hasSize(2)
                .extracting("componentType", "rightLabel", "userDefinedId")
                .contains(
                        tuple("ARROW_ACTIVE", Optional.of(TERMINAL_P + " " + ACTIVE_POWER_UNIT), null),
                        tuple("ARROW_REACTIVE", Optional.of(TERMINAL_Q + " " + REACTIVE_POWER_UNIT), null)
        );
        assertThat(actualFeederInfos).filteredOn(MeasurementFeederInfo.class::isInstance)
                .hasSize(3)
                .extracting("componentType", "rightLabel", "isValid", "isCritical", "isMissing")
                .containsExactly(
                        tuple("VALUE_CURRENT", Optional.of(measurementP + " " + ACTIVE_POWER_UNIT), isValid, isCritical, false),
                        tuple("VALUE_CURRENT", Optional.of(measurementQ + " " + REACTIVE_POWER_UNIT), isValid, isCritical, false),
                        tuple("VALUE_CURRENT", Optional.of(UNDEFINED_VALUE_SYMBOL), false, false, true)
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
    void testGetFeederInfosReturnsEmptyListWhenNonExistingTwoWindingTransformer() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederTwLeg feederMock = Mockito.mock(FeederTwLeg.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.TWO_WINDINGS_TRANSFORMER_LEG);
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
        assertThat(actualFeederInfos.get(5).getRightLabel()).isEqualTo(Optional.of(UNDEFINED_VALUE_SYMBOL));
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
        assertThat(actualFeederInfos.get(5).getRightLabel()).isEqualTo(Optional.of(UNDEFINED_VALUE_SYMBOL));
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

        assertThat(actualFeederInfos.get(0).getRightLabel()).isEqualTo(Optional.of(UNDEFINED_VALUE_SYMBOL));
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

        assertThat(actualFeederInfos).hasSize(6)
                .extracting("rightLabel")
                .containsExactly(
                        Optional.of(UNDEFINED_VALUE_SYMBOL),
                        Optional.of("? Mvar"),
                        Optional.of("? MW"),
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

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(ValueFeederInfo.class::isInstance)
                .extracting("rightLabel")
                .contains(Optional.of("100.0"));
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

        Measurements<?> measurementsMock = Mockito.mock(Measurements.class);
        Mockito.when(measurementsMock.getMeasurements()).thenReturn(List.of(measurementMock));
        Mockito.when(injectionMock.getExtension(Measurements.class)).thenReturn(measurementsMock);

        InjectionObservability<?> observabilityMock = Mockito.mock(InjectionObservability.class);
        Mockito.when(injectionMock.getExtension(InjectionObservability.class)).thenReturn(observabilityMock);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(ValueFeederInfo.class::isInstance)
                .extracting("rightLabel")
                .contains(Optional.of("100.0"));
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
        Mockito.when(m1.getType()).thenReturn(Measurement.Type.REACTIVE_POWER);
        Mockito.when(m1.getValue()).thenReturn(15.0);
        Mockito.when(m1.isValid()).thenReturn(true);

        Measurement m2 = Mockito.mock(Measurement.class);
        Mockito.when(m2.getType()).thenReturn(Measurement.Type.ACTIVE_POWER);
        Mockito.when(m2.getValue()).thenReturn(13.0);
        Mockito.when(m2.isValid()).thenReturn(true);

        Measurements<?> measurementsMock = Mockito.mock(Measurements.class);
        // Provide them in reverse order: REACTIVE_POWER, then ACTIVE_POWER. They should be sorted to ACTIVE_POWER, then REACTIVE_POWER.
        Mockito.when(measurementsMock.getMeasurements()).thenReturn(List.of(m1, m2));
        Mockito.when(injectionMock.getExtension(Measurements.class)).thenReturn(measurementsMock);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(MeasurementFeederInfo.class::isInstance)
                .extracting("rightLabel")
                .containsExactly(Optional.of("13.0 MW"), Optional.of("15.0 Mvar"));
    }

    @Test
    void testGetFeederInfosForBranchWithVoltageMeasurementButNoBranchObservabilityQuality() {
        FeederNode feederNodeMock = Mockito.mock(FeederNode.class);
        FeederWithSides feederMock = Mockito.mock(FeederWithSides.class);
        Mockito.when(feederMock.getFeederType()).thenReturn(FeederType.BRANCH);
        Mockito.when(feederMock.getSide()).thenReturn(NodeSide.ONE);
        Mockito.when(feederNodeMock.getFeeder()).thenReturn(feederMock);
        Mockito.when(feederNodeMock.getEquipmentId()).thenReturn("BRANCH_WITH_P_MEASUREMENT");

        Branch<?> branchMock = Mockito.mock(Branch.class);
        Terminal terminalMock = Mockito.mock(Terminal.class);
        Mockito.lenient().when(branchMock.getTerminal(TwoSides.ONE)).thenReturn(terminalMock);
        Mockito.when(networkMock.getBranch("BRANCH_WITH_P_MEASUREMENT")).thenReturn(branchMock);

        Measurement measurementMock = Mockito.mock(Measurement.class);
        Mockito.when(measurementMock.getType()).thenReturn(Measurement.Type.VOLTAGE);
        Mockito.when(measurementMock.getSide()).thenReturn(ThreeSides.ONE);
        Mockito.when(measurementMock.getValue()).thenReturn(400.0);
        Mockito.when(measurementMock.isValid()).thenReturn(true);

        Measurements<?> measurementsMock = Mockito.mock(Measurements.class);
        Mockito.when(measurementsMock.getMeasurements()).thenReturn(List.of(measurementMock));
        Mockito.when(branchMock.getExtension(Measurements.class)).thenReturn(measurementsMock);

        BranchObservability<?> observabilityMock = Mockito.mock(BranchObservability.class);
        Mockito.when(branchMock.getExtension(BranchObservability.class)).thenReturn(observabilityMock);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(MeasurementFeederInfo.class::isInstance)
                .extracting("rightLabel")
                .contains(Optional.of("400.0"));
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

        assertThat(actualFeederInfos).hasSize(6);
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

        assertThat(actualFeederInfos).hasSize(6);
        assertMissingMeasurements(actualFeederInfos, "? MW", "? Mvar", "?");
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

        Measurements<?> measurementsMock = Mockito.mock(Measurements.class);
        Mockito.when(measurementsMock.getMeasurements()).thenReturn(List.of(m1, m2));
        Mockito.when(branchMock.getExtension(Measurements.class)).thenReturn(measurementsMock);

        BranchObservability observabilityMock = Mockito.mock(BranchObservability.class);
        Mockito.when(branchMock.getExtension(BranchObservability.class)).thenReturn(observabilityMock);

        ObservabilityQuality<?> qualityMock = Mockito.mock(ObservabilityQuality.class);
        Mockito.when(qualityMock.isRedundant()).thenReturn(Optional.of(true));
        Mockito.when(observabilityMock.getQualityP1()).thenReturn(qualityMock);
        Mockito.when(observabilityMock.getQualityQ1()).thenReturn(qualityMock);

        List<FeederInfo> actualFeederInfos = provider.getFeederInfos(feederNodeMock);

        assertThat(actualFeederInfos).filteredOn(MeasurementFeederInfo.class::isInstance)
                .extracting("rightLabel")
                .containsExactly(Optional.of("10.0 MW"), Optional.of("11.0 Mvar"));
    }

    @ParameterizedTest
    @MethodSource("provideInjectionFamilies")
    void testMissingMeasurementsForEachInjectionFamily(Class<? extends Injection<?>> equipmentClass, String equipmentId,
                                                       List<String> expectedLabels, boolean emptyExtension) {
        Injection<?> injection = prepareInjection(equipmentClass, equipmentId);
        if (emptyExtension) {
            prepareMeasurements(injection, List.of());
        }
        FeederNode node = prepareFeederNode(equipmentId, FeederType.INJECTION, NodeSide.ONE);

        List<FeederInfo> infos = provider.getFeederInfos(node);

        assertThat(infos).hasSize(3 + expectedLabels.size());
        assertMissingMeasurements(infos, expectedLabels.toArray(String[]::new));
    }

    @ParameterizedTest
    @MethodSource("provideMissingSidedMeasurements")
    void testMissingMeasurementsForEachSidedEquipmentFamily(Class<? extends Branch<?>> equipmentClass, String equipmentId,
                                                           FeederType feederType, NodeSide side, boolean emptyExtension) {
        prepareUnitlessPowerMocks();
        Branch<?> branch = prepareBranch(equipmentClass, equipmentId);
        if (emptyExtension) {
            prepareMeasurements(branch, List.of());
        }

        List<FeederInfo> infos = provider.getFeederInfos(prepareFeederNode(equipmentId, feederType, side));

        assertThat(infos).hasSize(5);
        assertMissingMeasurements(infos, "?", "?", "?");
        assertThat(infos).filteredOn(info -> !(info instanceof MeasurementFeederInfo))
                .extracting("componentType").containsExactly("ARROW_ACTIVE", "ARROW_REACTIVE");
    }

    @ParameterizedTest
    @MethodSource("providePartialInjectionMeasurements")
    void testPartialInjectionAddsOnlyMissingTypes(Class<? extends Injection<?>> equipmentClass, Measurement.Type presentType, List<String> expectedLabels) {
        prepareUnitlessPowerMocks();
        Injection<?> injection = prepareInjection(equipmentClass, GENERATOR_WITH_PARTIAL_MEASUREMENTS);
        prepareMeasurements(injection, List.of(prepareMeasurement(presentType, MEASUREMENT_P, true, null)));

        List<MeasurementFeederInfo> infos = getMeasurementInfos(provider.getFeederInfos(
                prepareFeederNode(GENERATOR_WITH_PARTIAL_MEASUREMENTS, FeederType.INJECTION, NodeSide.ONE)));

        assertThat(infos).extracting(info -> info.getRightLabel().orElseThrow()).containsExactlyElementsOf(expectedLabels);
        assertThat(infos).filteredOn(info -> !info.isMissing()).hasSize(1)
                .extracting("componentType", "isValid", "isCritical", "isMissing")
                .containsExactly(tuple("VALUE_CURRENT", true, false, false));
        assertThat(infos).filteredOn(MeasurementFeederInfo::isMissing).hasSize(expectedLabels.size() - 1);
    }

    @ParameterizedTest
    @MethodSource("provideSidedEquipment")
    void testPartialMeasurementsAreCompletedPerNodeSide(Class<? extends Branch<?>> equipmentClass, String equipmentId,
                                                      FeederType feederType, NodeSide side) {
        prepareUnitlessPowerMocks();
        Branch<?> branch = prepareBranch(equipmentClass, equipmentId);
        prepareMeasurements(branch, List.of(
                prepareMeasurement(Measurement.Type.ACTIVE_POWER, MEASUREMENT_P1, true, ThreeSides.ONE),
                prepareMeasurement(Measurement.Type.VOLTAGE, MEASUREMENT_V, true, ThreeSides.ONE),
                prepareMeasurement(Measurement.Type.REACTIVE_POWER, MEASUREMENT_Q2, false, ThreeSides.TWO),
                prepareMeasurement(Measurement.Type.ACTIVE_POWER, 999D, true, null)));

        List<FeederInfo> infos = provider.getFeederInfos(prepareFeederNode(equipmentId, feederType, side));

        assertThat(getMeasurementInfos(infos)).extracting("componentType", "rightLabel", "isValid", "isCritical", "isMissing")
                .containsExactlyElementsOf(side == NodeSide.ONE ? List.of(
                        tuple("VALUE_CURRENT", Optional.of("16.0"), true, false, false),
                        tuple("VALUE_CURRENT", Optional.of("?"), false, false, true),
                        tuple("VALUE_CURRENT", Optional.of("225.1"), true, false, false)) : List.of(
                        tuple("VALUE_CURRENT", Optional.of("?"), false, false, true),
                        tuple("VALUE_CURRENT", Optional.of("19.0"), false, false, false),
                        tuple("VALUE_CURRENT", Optional.of("?"), false, false, true)));
    }

    @ParameterizedTest
    @EnumSource(value = NodeSide.class, names = {"ONE", "TWO"})
    void testHvdcCompletesOnlySelectedVscStation(NodeSide side) {
        prepareUnitlessPowerMocks();
        HvdcLine hvdcLine = Mockito.mock(HvdcLine.class);
        VscConverterStation stationOne = Mockito.mock(VscConverterStation.class);
        VscConverterStation stationTwo = Mockito.mock(VscConverterStation.class);
        Terminal terminal = prepareTerminal();
        Mockito.lenient().when(stationOne.getTerminal()).thenReturn(terminal);
        Mockito.lenient().when(stationTwo.getTerminal()).thenReturn(terminal);
        Mockito.lenient().doReturn(stationOne).when(hvdcLine).getConverterStation1();
        Mockito.lenient().doReturn(stationTwo).when(hvdcLine).getConverterStation2();
        Mockito.when(networkMock.getHvdcLine(HVDC_WITH_PARTIAL_MEASUREMENTS)).thenReturn(hvdcLine);
        // Station data is selected by its owner, even if its measurement side is null or different.
        prepareMeasurements(stationOne, List.of(prepareMeasurement(Measurement.Type.ACTIVE_POWER, MEASUREMENT_P1, true, null)));
        prepareMeasurements(stationTwo, List.of(prepareMeasurement(Measurement.Type.REACTIVE_POWER, MEASUREMENT_Q2, true, ThreeSides.ONE)));

        List<FeederInfo> infos = provider.getFeederInfos(prepareFeederNode(HVDC_WITH_PARTIAL_MEASUREMENTS, FeederType.HVDC, side));

        assertThat(getMeasurementInfos(infos)).extracting("rightLabel", "isMissing")
                .containsExactlyElementsOf(side == NodeSide.ONE ? List.of(
                        tuple(Optional.of("16.0"), false), tuple(Optional.of("?"), true), tuple(Optional.of("?"), true)) : List.of(
                        tuple(Optional.of("?"), true), tuple(Optional.of("19.0"), false), tuple(Optional.of("?"), true)));
    }

    @ParameterizedTest
    @MethodSource("provideMeasurementOrdering")
    void testOrderingWithMissingLabelsAndBaseInfos(Direction direction, boolean symmetry, List<String> expectedLabels) {
        prepareUnitlessPowerMocks();
        Mockito.when(svgParametersMock.getCurrentUnit()).thenReturn("");
        Mockito.lenient().when(svgParametersMock.isFeederInfoSymmetry()).thenReturn(symmetry);
        Injection<?> generator = prepareInjection(Generator.class, GENERATOR_WITH_PARTIAL_MEASUREMENTS);
        prepareMeasurements(generator, List.of(
                prepareMeasurement(Measurement.Type.VOLTAGE, MEASUREMENT_V, true, null),
                prepareMeasurement(Measurement.Type.ACTIVE_POWER, MEASUREMENT_P, true, null)));
        FeederNode node = prepareFeederNode(GENERATOR_WITH_PARTIAL_MEASUREMENTS, FeederType.INJECTION, NodeSide.ONE);
        Mockito.when(node.getDirection()).thenReturn(direction);

        List<FeederInfo> infos = provider.getFeederInfos(node);

        assertThat(infos).extracting(info -> info.getRightLabel().orElseThrow()).containsExactlyElementsOf(expectedLabels);
        assertMissingMeasurements(infos, "?");
    }

    @ParameterizedTest
    @MethodSource("provideBranchMeasurementOrdering")
    void testOrderingWithMissingLabelsAndFilteredBranchInfos(Direction direction, boolean symmetry, List<String> expectedLabels) {
        prepareUnitlessPowerMocks();
        Mockito.lenient().when(svgParametersMock.isFeederInfoSymmetry()).thenReturn(symmetry);
        Branch<?> line = prepareBranch(Line.class, LINE_WITH_PARTIAL_MEASUREMENTS);
        prepareMeasurements(line, List.of(
                prepareMeasurement(Measurement.Type.VOLTAGE, MEASUREMENT_V, true, ThreeSides.ONE),
                prepareMeasurement(Measurement.Type.ACTIVE_POWER, MEASUREMENT_P1, true, ThreeSides.ONE)));
        FeederNode node = prepareFeederNode(LINE_WITH_PARTIAL_MEASUREMENTS, FeederType.BRANCH, NodeSide.ONE);
        Mockito.when(node.getDirection()).thenReturn(direction);

        List<FeederInfo> infos = provider.getFeederInfos(node);

        assertThat(infos).extracting(info -> info.getRightLabel().orElseThrow()).containsExactlyElementsOf(expectedLabels);
        assertMissingMeasurements(infos, "?");
    }

    @Test
    void testSortingRetainsExtraTypesAndDuplicateMeasurements() {
        prepareUnitlessPowerMocks();
        Injection<?> generator = prepareInjection(Generator.class, GENERATOR_WITH_EXTRA_MEASUREMENTS);
        prepareMeasurements(generator, List.of(
                prepareMeasurement(Measurement.Type.CURRENT, 100D, true, null),
                prepareMeasurement(Measurement.Type.VOLTAGE, MEASUREMENT_V, true, null),
                prepareMeasurement(Measurement.Type.APPARENT_POWER, 99D, true, null),
                prepareMeasurement(Measurement.Type.OTHER, 98D, true, null),
                prepareMeasurement(Measurement.Type.FREQUENCY, 50D, true, null),
                prepareMeasurement(Measurement.Type.ANGLE, 90D, true, null),
                prepareMeasurement(Measurement.Type.REACTIVE_POWER, 14D, true, null),
                prepareMeasurement(Measurement.Type.REACTIVE_POWER, 15D, false, null)));

        List<FeederInfo> infos = provider.getFeederInfos(prepareFeederNode(GENERATOR_WITH_EXTRA_MEASUREMENTS, FeederType.INJECTION, NodeSide.ONE));

        assertThat(getMeasurementInfos(infos)).extracting("rightLabel", "isMissing")
                .containsExactly(tuple(Optional.of("?"), true),
                        tuple(Optional.of("14.0"), false), tuple(Optional.of("15.0"), false),
                        tuple(Optional.of("225.1"), false), tuple(Optional.of("90.0"), false),
                        tuple(Optional.of("99.0"), false), tuple(Optional.of("100.0"), false),
                        tuple(Optional.of("50.0"), false), tuple(Optional.of("98.0"), false));
        assertMissingMeasurements(infos, "?");
    }

    @ParameterizedTest
    @MethodSource("provideSidedEquipment")
    void testSidedMeasurementsDisplayActiveReactiveVoltageBeforeExtraTypes(Class<? extends Branch<?>> equipmentClass,
                                                                         String equipmentId, FeederType feederType, NodeSide side) {
        prepareUnitlessPowerMocks();
        Branch<?> equipment = prepareBranch(equipmentClass, equipmentId);
        ThreeSides measurementSide = ThreeSides.valueOf(side.name());
        prepareMeasurements(equipment, List.of(
                prepareMeasurement(Measurement.Type.CURRENT, 100D, true, measurementSide),
                prepareMeasurement(Measurement.Type.VOLTAGE, MEASUREMENT_V, true, measurementSide),
                prepareMeasurement(Measurement.Type.REACTIVE_POWER, MEASUREMENT_Q, false, measurementSide),
                prepareMeasurement(Measurement.Type.APPARENT_POWER, 99D, true, measurementSide),
                prepareMeasurement(Measurement.Type.ACTIVE_POWER, MEASUREMENT_P, true, measurementSide)));

        List<FeederInfo> infos = provider.getFeederInfos(prepareFeederNode(equipmentId, feederType, side));

        assertThat(getMeasurementInfos(infos)).extracting("rightLabel", "isMissing")
                .containsExactly(tuple(Optional.of("13.0"), false), tuple(Optional.of("14.0"), false),
                        tuple(Optional.of("225.1"), false), tuple(Optional.of("99.0"), false), tuple(Optional.of("100.0"), false));
    }

    @Test
    void testCompleteExtensionDoesNotAddMissingLabels() {
        prepareUnitlessPowerMocks();
        Injection<?> generator = prepareInjection(Generator.class, GENERATOR_WITH_MEASUREMENTS);
        prepareMeasurements(generator, List.of(
                prepareMeasurement(Measurement.Type.VOLTAGE, MEASUREMENT_V, true, null),
                prepareMeasurement(Measurement.Type.REACTIVE_POWER, MEASUREMENT_Q, false, null),
                prepareMeasurement(Measurement.Type.ACTIVE_POWER, MEASUREMENT_P, true, null)));

        List<FeederInfo> infos = provider.getFeederInfos(prepareFeederNode(GENERATOR_WITH_MEASUREMENTS, FeederType.INJECTION, NodeSide.ONE));

        assertThat(getMeasurementInfos(infos)).extracting("rightLabel", "isMissing")
                .containsExactly(tuple(Optional.of("13.0"), false), tuple(Optional.of("14.0"), false), tuple(Optional.of("225.1"), false));
    }

    @Test
    void testExistingInvalidNanCountsAsPresentAndIsNotMissing() {
        prepareUnitlessPowerMocks();
        Injection<?> generator = prepareInjection(Generator.class, GENERATOR_WITH_NAN_MEASUREMENT);
        prepareMeasurements(generator, List.of(prepareMeasurement(Measurement.Type.ACTIVE_POWER, Double.NaN, false, null)));

        List<MeasurementFeederInfo> infos = getMeasurementInfos(provider.getFeederInfos(
                prepareFeederNode(GENERATOR_WITH_NAN_MEASUREMENT, FeederType.INJECTION, NodeSide.ONE)));

        assertThat(infos).extracting("rightLabel", "isValid", "isCritical", "isMissing")
                .containsExactly(tuple(Optional.of("?"), false, false, false),
                        tuple(Optional.of("?"), false, false, true), tuple(Optional.of("?"), false, false, true));
        MeasurementStyleProvider styleProvider = new MeasurementStyleProvider();
        assertThat(styleProvider.getFeederInfoStyles(infos.getFirst())).containsExactly("sld-measurement-invalid");
        assertThat(styleProvider.getFeederInfoStyles(infos.get(1))).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideUndefinedSymbols")
    void testMissingLabelsUseConfiguredUndefinedSymbol(String symbol) {
        SvgParameters parameters = new SvgParameters().setUndefinedValueSymbol(symbol);
        MeasurementLabelProvider labelProvider = new MeasurementLabelProvider(networkMock, sldComponentLibraryMock, layoutParametersMock, parameters);

        List<FeederInfo> infos = labelProvider.getFeederInfos(prepareFeederNode(GENERATOR_WITHOUT_MEASUREMENTS, FeederType.INJECTION, NodeSide.ONE));

        assertMissingMeasurements(infos, symbol, symbol, symbol);
    }

    private static Stream<String> provideUndefinedSymbols() {
        return Stream.of("?", "—");
    }

    @Test
    void testVoltageUsesVoltagePrecisionWithoutChangingCriticality() {
        SvgParameters parameters = new SvgParameters().setPowerValuePrecision(0).setVoltageValuePrecision(2);
        MeasurementLabelProvider labelProvider = new MeasurementLabelProvider(networkMock, sldComponentLibraryMock, layoutParametersMock, parameters);
        Injection<?> generator = prepareInjection(Generator.class, GENERATOR_WITH_PARTIAL_MEASUREMENTS);
        prepareMeasurements(generator, List.of(
                prepareMeasurement(Measurement.Type.ACTIVE_POWER, 13.456D, true, null),
                prepareMeasurement(Measurement.Type.VOLTAGE, MEASUREMENT_V, true, null)));
        InjectionObservability<?> observability = Mockito.mock(InjectionObservability.class);
        ObservabilityQuality<?> quality = Mockito.mock(ObservabilityQuality.class);
        Mockito.lenient().when(quality.isRedundant()).thenReturn(Optional.of(false));
        Mockito.lenient().doReturn(quality).when(observability).getQualityV();
        Mockito.doReturn(observability).when(generator).getExtension(InjectionObservability.class);

        List<FeederInfo> infos = labelProvider.getFeederInfos(prepareFeederNode(GENERATOR_WITH_PARTIAL_MEASUREMENTS, FeederType.INJECTION, NodeSide.ONE));

        assertThat(getMeasurementInfos(infos)).extracting("rightLabel", "isCritical", "isMissing")
                .containsExactly(tuple(Optional.of("13"), false, false), tuple(Optional.of("—"), false, true),
                        tuple(Optional.of("225.12"), false, false));
    }

    @ParameterizedTest
    @EnumSource(value = Measurement.Type.class, names = {"ACTIVE_POWER", "REACTIVE_POWER"})
    void testMissingAndUnknownObservabilityQualityIsNotCritical(Measurement.Type type) {
        prepareUnitlessPowerMocks();
        Injection<?> generator = prepareInjection(Generator.class, GENERATOR_WITH_PARTIAL_MEASUREMENTS);
        prepareMeasurements(generator, List.of(prepareMeasurement(type, MEASUREMENT_P, true, null)));
        InjectionObservability<?> observability = Mockito.mock(InjectionObservability.class);
        Mockito.doReturn(observability).when(generator).getExtension(InjectionObservability.class);
        FeederNode node = prepareFeederNode(GENERATOR_WITH_PARTIAL_MEASUREMENTS, FeederType.INJECTION, NodeSide.ONE);

        assertThat(getMeasurementInfos(provider.getFeederInfos(node))).allSatisfy(info -> assertThat(info.isCritical()).isFalse());

        ObservabilityQuality<?> quality = Mockito.mock(ObservabilityQuality.class);
        Mockito.when(quality.isRedundant()).thenReturn(Optional.empty());
        if (type == Measurement.Type.ACTIVE_POWER) {
            Mockito.doReturn(quality).when(observability).getQualityP();
        } else {
            Mockito.doReturn(quality).when(observability).getQualityQ();
        }
        assertThat(getMeasurementInfos(provider.getFeederInfos(node))).allSatisfy(info -> assertThat(info.isCritical()).isFalse());
    }

    @ParameterizedTest
    @EnumSource(value = Measurement.Type.class, names = {"ANGLE", "APPARENT_POWER", "CURRENT", "FREQUENCY", "OTHER"})
    void testActualTypesOutsideExpectationsKeepTheirUnitlessRendering(Measurement.Type type) {
        Injection<?> generator = prepareInjection(Generator.class, GENERATOR_WITH_EXTRA_MEASUREMENTS);
        prepareMeasurements(generator, List.of(prepareMeasurement(type, 100D, true, null)));

        List<FeederInfo> infos = provider.getFeederInfos(prepareFeederNode(GENERATOR_WITH_EXTRA_MEASUREMENTS, FeederType.INJECTION, NodeSide.ONE));

        assertMissingMeasurements(infos, "? MW", "? Mvar", "?");
        assertThat(getMeasurementInfos(infos)).filteredOn(info -> !info.isMissing())
                .extracting("componentType", "rightLabel", "isValid", "isCritical")
                .containsExactly(tuple("VALUE_CURRENT", Optional.of("100.0"), true, false));
    }

    @ParameterizedTest
    @MethodSource("provideExcludedEquipment")
    void testUnlistedEquipmentDoesNotGetMissingLabels(Class<? extends Identifiable<?>> equipmentClass, String equipmentId, FeederType feederType) {
        Identifiable<?> equipment = Mockito.mock(equipmentClass);
        Terminal terminal = prepareTerminal();
        if (equipment instanceof Injection<?> injection) {
            Mockito.when(injection.getTerminal()).thenReturn(terminal);
            Mockito.doReturn(injection).when(networkMock).getIdentifiable(equipmentId);
        } else if (equipment instanceof Branch<?> branch) {
            Mockito.when(branch.getTerminal(TwoSides.ONE)).thenReturn(terminal);
            Mockito.when(networkMock.getBranch(equipmentId)).thenReturn(branch);
        }
        FeederNode node = prepareFeederNode(equipmentId, feederType, NodeSide.ONE);

        assertThat(getMeasurementInfos(provider.getFeederInfos(node))).isEmpty();

        prepareMeasurements(equipment, List.of(prepareMeasurement(Measurement.Type.ACTIVE_POWER, MEASUREMENT_P, true, ThreeSides.ONE)));
        assertThat(getMeasurementInfos(provider.getFeederInfos(node))).extracting("rightLabel", "isMissing")
                .containsExactly(tuple(Optional.of("13.0 MW"), false));
    }

    @ParameterizedTest
    @EnumSource(value = NodeSide.class, names = {"ONE", "TWO"})
    void testLccHvdcKeepsActualMeasurementsWithoutMissingLabels(NodeSide side) {
        HvdcLine hvdcLine = Mockito.mock(HvdcLine.class);
        LccConverterStation station = Mockito.mock(LccConverterStation.class);
        Terminal terminal = prepareTerminal();
        Mockito.when(station.getTerminal()).thenReturn(terminal);
        Mockito.lenient().doReturn(station).when(hvdcLine).getConverterStation1();
        Mockito.lenient().doReturn(station).when(hvdcLine).getConverterStation2();
        Mockito.when(networkMock.getHvdcLine(HVDC_WITH_MEASUREMENTS)).thenReturn(hvdcLine);
        FeederNode node = prepareFeederNode(HVDC_WITH_MEASUREMENTS, FeederType.HVDC, side);

        assertThat(getMeasurementInfos(provider.getFeederInfos(node))).isEmpty();

        prepareMeasurements(station, List.of(prepareMeasurement(Measurement.Type.ACTIVE_POWER, MEASUREMENT_P, true, null)));
        assertThat(getMeasurementInfos(provider.getFeederInfos(node))).extracting("rightLabel", "isMissing")
                .containsExactly(tuple(Optional.of("13.0 MW"), false));
    }

    @ParameterizedTest
    @EnumSource(value = FeederType.class, names = {"GROUND", "FICTITIOUS"})
    void testDefaultFeederTypesRemainEmpty(FeederType feederType) {
        assertThat(provider.getFeederInfos(prepareFeederNode("DEFAULT_FEEDER", feederType, NodeSide.ONE))).isEmpty();
    }

    @Test
    void testThreeWindingTransformerDefaultPathIsUnchanged() {
        ThreeWindingsTransformer transformer = Mockito.mock(ThreeWindingsTransformer.class);
        Mockito.when(networkMock.getThreeWindingsTransformer(THREE_WINDINGS_TRANSFORMER_WITH_MEASUREMENTS)).thenReturn(transformer);
        Terminal terminal = prepareTerminal();
        Mockito.when(transformer.getTerminal(ThreeSides.ONE)).thenReturn(terminal);
        ThreeWindingsTransformer.Leg leg = Mockito.mock(ThreeWindingsTransformer.Leg.class);
        for (ThreeSides side : ThreeSides.values()) {
            Mockito.when(transformer.getLeg(side)).thenReturn(leg);
        }
        prepareMeasurements(transformer, List.of(prepareMeasurement(Measurement.Type.ACTIVE_POWER, MEASUREMENT_P, true, ThreeSides.ONE)));
        FeederNode node = prepareFeederNode(THREE_WINDINGS_TRANSFORMER_WITH_MEASUREMENTS, FeederType.THREE_WINDINGS_TRANSFORMER_LEG, NodeSide.ONE);
        FeederTwLeg feeder = (FeederTwLeg) node.getFeeder();
        VoltageLevelInfos voltageLevel = new VoltageLevelInfos("VL", "VL", 225D);
        Mockito.when(feeder.getOwnVoltageLevelInfos()).thenReturn(voltageLevel);
        Mockito.when(feeder.getVoltageLevelInfos()).thenReturn(voltageLevel);

        List<FeederInfo> infos = provider.getFeederInfos(node);

        assertThat(getMeasurementInfos(infos)).isEmpty();
        assertThat(infos).extracting("componentType").containsExactly("ARROW_ACTIVE", "ARROW_REACTIVE");
    }

    @Test
    void testTeePointLineDefaultPathIsUnchanged() {
        Line line = Mockito.mock(Line.class);
        Mockito.when(networkMock.getLine(TEE_POINT_LINE_WITH_MEASUREMENTS)).thenReturn(line);
        Terminal terminal = prepareTerminal();
        Mockito.when(line.getTerminal1()).thenReturn(terminal);
        prepareMeasurements(line, List.of(prepareMeasurement(Measurement.Type.ACTIVE_POWER, MEASUREMENT_P, true, ThreeSides.ONE)));
        FeederNode node = Mockito.mock(FeederNode.class);
        FeederTeePointLeg feeder = Mockito.mock(FeederTeePointLeg.class);
        Mockito.when(feeder.getFeederType()).thenReturn(FeederType.TEE_POINT_LEG);
        VoltageLevelInfos voltageLevel = new VoltageLevelInfos("VL", "VL", 225D);
        Mockito.when(feeder.getOwnVoltageLevelInfos()).thenReturn(voltageLevel);
        Mockito.when(feeder.getVoltageLevelInfos()).thenReturn(voltageLevel);
        Mockito.when(node.getFeeder()).thenReturn(feeder);
        Mockito.when(node.getEquipmentId()).thenReturn(TEE_POINT_LINE_WITH_MEASUREMENTS);

        List<FeederInfo> infos = provider.getFeederInfos(node);

        assertThat(getMeasurementInfos(infos)).isEmpty();
        assertThat(infos).extracting("componentType").containsExactly("ARROW_ACTIVE", "ARROW_REACTIVE", "VALUE_CURRENT");
    }

    @Test
    void testBusbarsAndRegulatingTerminalsDoNotAddNodeLabels() {
        BusNode busNode = new BusNode("BUSBAR", "BUSBAR", false);
        assertThat(provider.getBusInfo(busNode)).isEmpty();
        assertThat(provider.getNodeLabels(busNode, Direction.TOP)).extracting("label").containsExactly("BUSBAR");

        Generator generator = Mockito.mock(Generator.class);
        Mockito.lenient().when(generator.getRegulatingTerminal()).thenReturn(Mockito.mock(Terminal.class));
        Terminal terminal = prepareTerminal();
        Mockito.when(generator.getTerminal()).thenReturn(terminal);
        Mockito.doReturn(generator).when(networkMock).getIdentifiable(GENERATOR_WITHOUT_MEASUREMENTS);
        assertMissingMeasurements(provider.getFeederInfos(prepareFeederNode(GENERATOR_WITHOUT_MEASUREMENTS, FeederType.INJECTION, NodeSide.ONE)),
                "? MW", "? Mvar", "?");
        Mockito.verify(generator, Mockito.never()).getRegulatingTerminal();
    }

    private static List<MeasurementFeederInfo> getMeasurementInfos(List<FeederInfo> infos) {
        return infos.stream().filter(MeasurementFeederInfo.class::isInstance).map(MeasurementFeederInfo.class::cast).toList();
    }

    private static void assertMissingMeasurements(List<FeederInfo> infos, String... labels) {
        List<MeasurementFeederInfo> missingInfos = getMeasurementInfos(infos).stream().filter(MeasurementFeederInfo::isMissing).toList();
        assertThat(missingInfos).extracting("componentType", "rightLabel", "isValid", "isCritical", "isMissing")
                .containsExactlyElementsOf(Stream.of(labels).map(label -> tuple("VALUE_CURRENT", Optional.of(label), false, false, true)).toList());
        assertThat(missingInfos).allSatisfy(info -> {
            assertThat(info.getValue()).isNaN();
            assertThat(info.getDirection()).isEqualTo(LabelProvider.LabelDirection.NONE);
            assertThat(info.getLeftLabel()).isEmpty();
            assertThat(info.getUserDefinedId()).isNull();
            assertThat(new MeasurementStyleProvider().getFeederInfoStyles(info)).isEmpty();
        });
    }

    private void prepareUnitlessPowerMocks() {
        Mockito.lenient().when(svgParametersMock.getActivePowerUnit()).thenReturn("");
        Mockito.lenient().when(svgParametersMock.getReactivePowerUnit()).thenReturn("");
    }

    private Terminal prepareTerminal() {
        Terminal terminal = Mockito.mock(Terminal.class);
        Mockito.lenient().when(terminal.getP()).thenReturn(TERMINAL_P);
        Mockito.lenient().when(terminal.getQ()).thenReturn(TERMINAL_Q);
        Mockito.lenient().when(terminal.getI()).thenReturn(TERMINAL_I);
        return terminal;
    }

    private Injection<?> prepareInjection(Class<? extends Injection<?>> equipmentClass, String equipmentId) {
        Injection<?> injection = Mockito.mock(equipmentClass);
        Terminal terminal = prepareTerminal();
        Mockito.when(injection.getTerminal()).thenReturn(terminal);
        Mockito.doReturn(injection).when(networkMock).getIdentifiable(equipmentId);
        return injection;
    }

    private Branch<?> prepareBranch(Class<? extends Branch<?>> equipmentClass, String equipmentId) {
        Branch<?> branch = Mockito.mock(equipmentClass);
        Terminal terminal = prepareTerminal();
        Mockito.lenient().when(branch.getTerminal(TwoSides.ONE)).thenReturn(terminal);
        Mockito.lenient().when(branch.getTerminal(TwoSides.TWO)).thenReturn(terminal);
        Mockito.when(networkMock.getBranch(equipmentId)).thenReturn(branch);
        return branch;
    }

    private FeederNode prepareFeederNode(String equipmentId, FeederType feederType, NodeSide side) {
        FeederNode node = Mockito.mock(FeederNode.class);
        Feeder feeder;
        if (feederType == FeederType.TWO_WINDINGS_TRANSFORMER_LEG || feederType == FeederType.THREE_WINDINGS_TRANSFORMER_LEG) {
            FeederTwLeg leg = Mockito.mock(FeederTwLeg.class);
            Mockito.lenient().when(leg.getSide()).thenReturn(side);
            feeder = leg;
        } else if (feederType == FeederType.BRANCH || feederType == FeederType.HVDC) {
            FeederWithSides sidedFeeder = Mockito.mock(FeederWithSides.class);
            Mockito.lenient().when(sidedFeeder.getSide()).thenReturn(side);
            feeder = sidedFeeder;
        } else {
            feeder = Mockito.mock(Feeder.class);
        }
        Mockito.when(feeder.getFeederType()).thenReturn(feederType);
        Mockito.when(node.getFeeder()).thenReturn(feeder);
        Mockito.lenient().when(node.getEquipmentId()).thenReturn(equipmentId);
        return node;
    }

    private Measurement prepareMeasurement(Measurement.Type type, double value, boolean valid, ThreeSides side) {
        Measurement measurement = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurement.getType()).thenReturn(type);
        Mockito.lenient().when(measurement.getValue()).thenReturn(value);
        Mockito.lenient().when(measurement.isValid()).thenReturn(valid);
        Mockito.lenient().when(measurement.getSide()).thenReturn(side);
        return measurement;
    }

    private void prepareMeasurements(Identifiable<?> equipment, List<Measurement> readings) {
        Measurements<?> measurements = Mockito.mock(Measurements.class);
        Mockito.lenient().when(measurements.getMeasurements()).thenReturn(readings);
        Mockito.lenient().doReturn(measurements).when(equipment).getExtension(Measurements.class);
    }

    private void prepareMocks() {
        ValueFormatter valueFormatter = new ValueFormatter(1, 1, 1, 1, 1, Locale.US, UNDEFINED_VALUE_SYMBOL);
        Mockito.lenient().when(svgParametersMock.createValueFormatter()).thenReturn(valueFormatter);
        Mockito.lenient().when(svgParametersMock.getCurrentUnit()).thenReturn(CURRENT_UNIT);
        Mockito.lenient().when(svgParametersMock.getActivePowerUnit()).thenReturn(ACTIVE_POWER_UNIT);
        Mockito.lenient().when(svgParametersMock.getReactivePowerUnit()).thenReturn(REACTIVE_POWER_UNIT);

        Generator injectionNoMeasurementsMock = Mockito.mock(Generator.class);
        Generator injectionWithMeasurementsMock = Mockito.mock(Generator.class);
        Generator injectionWithMeasurementsNoObservabilityMock = Mockito.mock(Generator.class);
        Line branchNoMeasurementsMock = Mockito.mock(Line.class);
        Line branchWithMeasurementsMock = Mockito.mock(Line.class);
        TwoWindingsTransformer twoWindingsTransformerNoMeasurementsMock = Mockito.mock(TwoWindingsTransformer.class);
        TwoWindingsTransformer twoWindingsTransformerWithMeasurementsMock = Mockito.mock(TwoWindingsTransformer.class);
        HvdcLine hvdcNoMeasurementsMock = Mockito.mock(HvdcLine.class);
        HvdcLine hvdcWithMeasurementsMock = Mockito.mock(HvdcLine.class);

        Terminal terminalMock = Mockito.mock(Terminal.class);
        Mockito.lenient().when(terminalMock.getP()).thenReturn(TERMINAL_P);
        Mockito.lenient().when(terminalMock.getQ()).thenReturn(TERMINAL_Q);
        Mockito.lenient().when(terminalMock.getI()).thenReturn(TERMINAL_I);

        prepareInjectionMocks(injectionNoMeasurementsMock, injectionWithMeasurementsMock, injectionWithMeasurementsNoObservabilityMock, terminalMock);

        prepareBranchMocks(branchNoMeasurementsMock, branchWithMeasurementsMock, terminalMock);

        prepareBranchMocks(twoWindingsTransformerNoMeasurementsMock, twoWindingsTransformerWithMeasurementsMock, terminalMock);

        prepareHvdcMocks(hvdcNoMeasurementsMock, hvdcWithMeasurementsMock, terminalMock);

        Mockito.lenient().when(networkMock.getIdentifiable(GENERATOR_WITHOUT_MEASUREMENTS)).thenReturn((Identifiable) injectionNoMeasurementsMock);
        Mockito.lenient().when(networkMock.getIdentifiable(GENERATOR_WITH_MEASUREMENTS)).thenReturn((Identifiable) injectionWithMeasurementsMock);
        Mockito.lenient().when(networkMock.getIdentifiable(GENERATOR_WITH_MEASUREMENTS_NO_OBSERVABILITY)).thenReturn((Identifiable) injectionWithMeasurementsNoObservabilityMock);
        Mockito.lenient().when(networkMock.getBranch(BRANCH_WITHOUT_MEASUREMENTS)).thenReturn(branchNoMeasurementsMock);
        Mockito.lenient().when(networkMock.getBranch(BRANCH_WITH_MEASUREMENTS)).thenReturn(branchWithMeasurementsMock);
        Mockito.lenient().when(networkMock.getBranch(TWO_WINDINGS_TRANSFORMER_LEG_WITHOUT_MEASUREMENTS)).thenReturn(twoWindingsTransformerNoMeasurementsMock);
        Mockito.lenient().when(networkMock.getBranch(TWO_WINDINGS_TRANSFORMER_LEG_WITH_MEASUREMENTS)).thenReturn(twoWindingsTransformerWithMeasurementsMock);
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

        VscConverterStation hvdcConverterStationWithoutMeasurementsOne = Mockito.mock(VscConverterStation.class);
        Mockito.lenient().when(hvdcConverterStationWithoutMeasurementsOne.getTerminal()).thenReturn(terminalMock);

        VscConverterStation hvdcConverterStationWithoutMeasurementsTwo = Mockito.mock(VscConverterStation.class);
        Mockito.lenient().when(hvdcConverterStationWithoutMeasurementsTwo.getTerminal()).thenReturn(terminalMock);

        VscConverterStation hvdcConverterStationWithMeasurementsOne = Mockito.mock(VscConverterStation.class);
        Mockito.lenient().when(hvdcConverterStationWithMeasurementsOne.getTerminal()).thenReturn(terminalMock);
        Mockito.lenient().when(hvdcConverterStationWithMeasurementsOne.getExtension(Measurements.class)).thenReturn(measurementsSideOneMock);
        Mockito.lenient().when(hvdcConverterStationWithMeasurementsOne.getExtension(InjectionObservability.class)).thenReturn(injectionObservabilitySideOneMock);

        VscConverterStation hvdcConverterStationWithMeasurementsTwo = Mockito.mock(VscConverterStation.class);
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

    private void prepareInjectionMocks(Injection<?> injectionNoMeasurementsMock, Injection<?> injectionWithMeasurementsMock,
                                       Injection<?> injectionWithMeasurementsNoObservabilityMock, Terminal terminalMock) {
        Measurement measurementPMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementPMock.getType()).thenReturn(Measurement.Type.ACTIVE_POWER);
        Mockito.lenient().when(measurementPMock.getValue()).thenReturn(MEASUREMENT_P);
        Mockito.lenient().when(measurementPMock.isValid()).thenReturn(true);
        Measurement measurementQMock = Mockito.mock(Measurement.class);
        Mockito.lenient().when(measurementQMock.getType()).thenReturn(Measurement.Type.REACTIVE_POWER);
        Mockito.lenient().when(measurementQMock.getValue()).thenReturn(MEASUREMENT_Q);
        Mockito.lenient().when(measurementQMock.isValid()).thenReturn(false);
        Measurements<?> measurementsMock = Mockito.mock(Measurements.class);
        Mockito.lenient().when(measurementsMock.getMeasurements()).thenReturn(List.of(measurementPMock, measurementQMock));

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
