/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.utils;

import com.powsybl.sld.layout.*;

/**
 * @author Kevin Le Saulnier <kevin.le-saulnier at rte-france.com>
 */
//TODO: to remove once SmartVoltageLevelLayoutFactory allow us to pass PositionVoltageLevelLayoutFactoryParameters to VoltageLevelLayoutFactory
public interface CustomVoltageLevelLayoutFactoryCreator extends VoltageLevelLayoutFactoryCreator {
    static VoltageLevelLayoutFactoryCreator newCustomVoltageLevelLayoutFactoryCreator() {
        return CustomVoltageLevelLayoutFactory::new;
    }
}
