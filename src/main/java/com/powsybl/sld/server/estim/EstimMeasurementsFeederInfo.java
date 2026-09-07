/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.sld.server.estim;

import com.powsybl.sld.svg.LabelProvider;
import com.powsybl.sld.svg.ValueFeederInfo;
import lombok.Getter;

import java.util.function.BiFunction;

/**
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
@Getter
public class EstimMeasurementsFeederInfo extends ValueFeederInfo {

    private final boolean isValid;
    private final boolean isCritical;

    public EstimMeasurementsFeederInfo(String componentType, LabelProvider.LabelDirection labelDirection,
                                       double value, String unit, BiFunction<Double, String, String> formatter,
                                       boolean isValid, boolean isCritical) {
        super(componentType, labelDirection, value, unit, formatter);
        this.isValid = isValid;
        this.isCritical = isCritical;
    }
}
