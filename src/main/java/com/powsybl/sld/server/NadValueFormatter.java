/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.diagram.util.ValueFormatter;

import java.util.Locale;

public class NadValueFormatter extends ValueFormatter {

    public NadValueFormatter(int powerValuePrecision, int voltageValuePrecision, int currentValuePrecision, int angleValuePrecision, int percentageValuePrecision, Locale locale, String undefinedValueSymbol) {
        super(powerValuePrecision, voltageValuePrecision, currentValuePrecision, angleValuePrecision, percentageValuePrecision, Locale.US, undefinedValueSymbol);
    }

}
