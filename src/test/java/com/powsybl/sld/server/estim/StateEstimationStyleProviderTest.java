/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.sld.server.estim;

import com.powsybl.sld.svg.FeederInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
class StateEstimationStyleProviderTest {

    private StateEstimationStyleProvider provider;

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
        provider = new StateEstimationStyleProvider();
    }

    @ParameterizedTest
    @MethodSource("provideEstimMeasurementsInfos")
    void testGetFeederInfoStylesReturnsExpectedStyles(boolean isValid, boolean isCritical, List<String> expectedStyles) {
        EstimMeasurementsFeederInfo feederInfoMock = Mockito.mock(EstimMeasurementsFeederInfo.class);
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
}
