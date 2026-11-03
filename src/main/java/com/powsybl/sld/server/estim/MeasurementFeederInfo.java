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

import java.util.function.DoubleFunction;

/**
 * Measurement value and display state. Missing measurements are distinguished explicitly from
 * existing invalid measurements, which can also have an undefined value.
 *
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
@Getter
public class MeasurementFeederInfo extends ValueFeederInfo {

    private final boolean isValid;
    private final boolean isCritical;
    private final boolean isMissing;

    public MeasurementFeederInfo(String componentType, LabelProvider.LabelDirection labelDirection,
                                 double value, DoubleFunction<String> formatter,
                                 boolean isValid, boolean isCritical) {
        this(componentType, labelDirection, value, formatter, isValid, isCritical, false);
    }

    MeasurementFeederInfo(String componentType, LabelProvider.LabelDirection labelDirection,
                          double value, DoubleFunction<String> formatter,
                          boolean isValid, boolean isCritical, boolean isMissing) {
        super(componentType, labelDirection, value, formatter);
        this.isValid = isValid;
        this.isCritical = isCritical;
        this.isMissing = isMissing;
    }
}
