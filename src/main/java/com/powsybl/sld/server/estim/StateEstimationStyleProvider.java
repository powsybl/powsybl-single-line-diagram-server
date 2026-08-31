/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.sld.server.estim;

import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.styles.EmptyStyleProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
public class StateEstimationStyleProvider extends EmptyStyleProvider {

    private static final String VALID_MEASUREMENT_CSS = "sld-measurement-valid";
    private static final String INVALID_MEASUREMENT_CSS = "sld-measurement-invalid";
    private static final String CRITICAL_MEASUREMENT_CSS = "sld-measurement-critical";

    @Override
    public List<String> getFeederInfoStyles(FeederInfo feederInfo) {
        if (feederInfo instanceof EstimMeasurementsFeederInfo estimMeasurementsFeederInfo) {
            return getMeasurementsStyles(estimMeasurementsFeederInfo);
        }
        return Collections.emptyList();
    }

    private List<String> getMeasurementsStyles(EstimMeasurementsFeederInfo estimMeasurementsFeederInfo) {
        List<String> styles = new ArrayList<>();
        styles.add(estimMeasurementsFeederInfo.isValid() ? VALID_MEASUREMENT_CSS : INVALID_MEASUREMENT_CSS);
        if (estimMeasurementsFeederInfo.isCritical()) {
            styles.add(CRITICAL_MEASUREMENT_CSS);
        }
        return styles;
    }
}
