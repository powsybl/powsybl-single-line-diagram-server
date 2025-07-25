/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.google.auto.service.AutoService;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.library.SldResourcesComponentLibrary;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@AutoService(SldComponentLibrary.class)
public class GridSuiteAndConvergenceComponentLibrary extends SldResourcesComponentLibrary {
    public static final String NAME = "GridSuiteAndConvergence";

    public GridSuiteAndConvergenceComponentLibrary() {
        super(NAME, "/ConvergenceLibrary", "/GridSuiteLibrary");
    }
}
