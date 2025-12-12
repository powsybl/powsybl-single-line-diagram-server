/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.utils;

import com.powsybl.sld.svg.BusLegendInfo;
import com.powsybl.sld.svg.styles.EmptyStyleProvider;
import com.powsybl.sld.svg.styles.StyleClassConstants;

import java.util.Collections;
import java.util.List;

import static com.powsybl.sld.server.CommonLegendWriter.*;

/**
 * @author Kevin Le Saulnier <kevin.lesaulnier at rte-france.com>
 */
public class BusLegendStyleProvider extends EmptyStyleProvider {
    private static final String PRODUCTION = StyleClassConstants.STYLE_PREFIX + KEY_PRODUCTION;
    private static final String CONSUMPTION = StyleClassConstants.STYLE_PREFIX + KEY_CONSUMPTION;
    private static final String ICC = StyleClassConstants.STYLE_PREFIX + "icc";

    @Override
    public List<String> getBusLegendCaptionStyles(BusLegendInfo.Caption caption) {
        return switch (caption.type()) {
            case KEY_VOLTAGE -> List.of(StyleClassConstants.VOLTAGE);
            case KEY_ANGLE -> List.of(StyleClassConstants.ANGLE);
            case KEY_PRODUCTION -> List.of(PRODUCTION);
            case KEY_CONSUMPTION -> List.of(CONSUMPTION);
            case "icc" -> List.of(ICC);
            default -> Collections.emptyList();
        };
    }
}
