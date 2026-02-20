/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.diagram.util.ValueFormatter;
import com.powsybl.iidm.network.*;
import com.powsybl.nad.model.ThreeWtEdge;
import com.powsybl.nad.svg.EdgeInfo;
import com.powsybl.nad.svg.LabelProviderParameters;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.DefaultLabelProvider;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public class NadValueFormatter extends ValueFormatter {

    public NadValueFormatter(int powerValuePrecision, int voltageValuePrecision, int currentValuePrecision, int angleValuePrecision, int percentageValuePrecision, Locale locale, String undefinedValueSymbol) {
        super(powerValuePrecision, voltageValuePrecision, currentValuePrecision, angleValuePrecision, percentageValuePrecision, Locale.getDefault(), undefinedValueSymbol);
    }
}
