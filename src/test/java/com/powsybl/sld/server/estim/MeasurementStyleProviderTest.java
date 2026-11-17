/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.sld.server.estim;

import com.powsybl.diagram.util.ValueFormatter;
import com.powsybl.sld.library.SldComponentTypeName;
import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.LabelProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
class MeasurementStyleProviderTest {

    private MeasurementStyleProvider provider;

    private static Stream<Arguments> provideEstimMeasurementsInfos() {
        return Stream.of(
                Arguments.of(true, true, List.of("sld-measurement-valid", "sld-measurement-critical")),
                Arguments.of(false, false, List.of("sld-measurement-invalid")),
                Arguments.of(true, false, List.of("sld-measurement-valid")),
                Arguments.of(false, true, List.of("sld-measurement-invalid", "sld-measurement-critical"))
        );
    }

    @BeforeEach
    void setUp() {
        provider = new MeasurementStyleProvider();
    }

    @ParameterizedTest
    @MethodSource("provideEstimMeasurementsInfos")
    void testGetFeederInfoStylesReturnsExpectedStyles(boolean isValid, boolean isCritical, List<String> expectedStyles) {
        MeasurementFeederInfo feederInfoMock = Mockito.mock(MeasurementFeederInfo.class);
        when(feederInfoMock.isValid()).thenReturn(isValid);
        when(feederInfoMock.isCritical()).thenReturn(isCritical);

        List<String> actualList = provider.getFeederInfoStyles(feederInfoMock);

        assertThat(actualList).containsExactlyElementsOf(expectedStyles);
    }

    @Test
    void testGetFeederInfoStylesReturnsEmptyListWhenNotEstimFeederInfos() {
        FeederInfo feederInfoMock = Mockito.mock(FeederInfo.class);

        List<String> actualList = provider.getFeederInfoStyles(feederInfoMock);

        assertThat(actualList).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("provideEstimMeasurementsInfos")
    void testMissingMeasurementsHaveNoStylesRegardlessOfValidityAndCriticality(boolean isValid, boolean isCritical, List<String> ignoredStyles) {
        MeasurementFeederInfo feederInfoMock = Mockito.mock(MeasurementFeederInfo.class);
        when(feederInfoMock.isMissing()).thenReturn(true);
        Mockito.lenient().when(feederInfoMock.isValid()).thenReturn(isValid);
        Mockito.lenient().when(feederInfoMock.isCritical()).thenReturn(isCritical);

        assertThat(provider.getFeederInfoStyles(feederInfoMock)).isEmpty();
    }

    @Test
    void testMissingFlagDistinguishesAbsentAndExistingInvalidNanMeasurements() {
        ValueFormatter formatter = new ValueFormatter(1, 1, 1, 1, 1, Locale.US, "?");
        MeasurementFeederInfo invalidInfo = new MeasurementFeederInfo(SldComponentTypeName.VALUE_CURRENT,
                LabelProvider.LabelDirection.NONE, Double.NaN, formatter::formatPower, false, false);
        MeasurementFeederInfo missingInfo = new MeasurementFeederInfo(SldComponentTypeName.VALUE_CURRENT,
                LabelProvider.LabelDirection.NONE, Double.NaN, formatter::formatPower, false, false, true);

        assertThat(invalidInfo.isMissing()).isFalse();
        assertThat(missingInfo.isMissing()).isTrue();
        assertThat(invalidInfo.getRightLabel()).isEqualTo(missingInfo.getRightLabel()).contains("?");
        assertThat(provider.getFeederInfoStyles(invalidInfo)).containsExactly("sld-measurement-invalid");
        assertThat(provider.getFeederInfoStyles(missingInfo)).isEmpty();
    }
}
