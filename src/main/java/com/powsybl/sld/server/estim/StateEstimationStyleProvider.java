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

import java.util.Collections;
import java.util.List;

/**
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
public class StateEstimationStyleProvider extends EmptyStyleProvider {

    private static final String STATE_ESTIMATION_SLD_CSS_FILE = "css/state-estimation-sld.css";

    @Override
    public List<String> getCssFilenames() {
        return Collections.singletonList(STATE_ESTIMATION_SLD_CSS_FILE);
    }

    @Override
    public List<String> getFeederInfoStyles(FeederInfo info) {
        return info.getUserDefinedId() != null ? Collections.singletonList(info.getUserDefinedId()) : Collections.emptyList();
    }
}
