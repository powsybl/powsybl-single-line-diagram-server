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
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
class StateEstimationStyleProviderTest {

    private StateEstimationStyleProvider provider;

    @BeforeEach
    void setUp() {
        provider = new StateEstimationStyleProvider();
    }

    @Test
    void testGetCssFilenamesReturnsExpectedList() {
        List<String> actualList = provider.getCssFilenames();

        assertThat(actualList)
                .containsExactly("css/state-estimation-sld.css")
                .size().isEqualTo(1);
    }

    @Test
    void testGetFeederInfoStylesReturnsListOfUserDefinedIdWhenNotNull() {
        String userDefinedId = "hello world";
        FeederInfo feederInfoMock = Mockito.mock(FeederInfo.class);
        Mockito.when(feederInfoMock.getUserDefinedId()).thenReturn(userDefinedId);

        List<String> actualList = provider.getFeederInfoStyles(feederInfoMock);

        assertThat(actualList)
                .containsExactly(userDefinedId)
                .size().isEqualTo(1);
    }

    @Test
    void testGetFeederInfoStylesReturnsEmptyListWhenUserDefinedIdNull() {
        FeederInfo feederInfoMock = Mockito.mock(FeederInfo.class);
        Mockito.when(feederInfoMock.getUserDefinedId()).thenReturn(null);

        List<String> actualList = provider.getFeederInfoStyles(feederInfoMock);

        assertThat(actualList).isEmpty();
    }
}
