/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto;

import java.util.List;
import java.util.Map;

import com.powsybl.commons.config.BaseVoltageConfig;
import com.powsybl.sld.server.GridSuiteAndConvergenceComponentLibrary;
import com.powsybl.sld.server.utils.DiagramConstants;
import com.powsybl.sld.server.utils.SldDisplayMode;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SldRequestInfos {
    private boolean useName = false;
    private boolean centerLabel = false;
    private boolean diagonalLabel = false;
    private boolean topologicalColoring = false;
    private String componentLibrary = GridSuiteAndConvergenceComponentLibrary.NAME;
    private String substationLayout = DiagramConstants.SUBSTATION_LAYOUT_HORIZONTAL;
    private SldDisplayMode sldDisplayMode = SldDisplayMode.STATE_VARIABLE;
    private String language = "en";
    private List<CurrentLimitViolationInfos> currentLimitViolationsInfos;
    private List<BaseVoltageConfig> baseVoltagesConfigInfos;
    Map<String, Double> busIdToIccValues;

    public void setComponentLibrary(String componentLibrary) {
        this.componentLibrary = (componentLibrary == null || componentLibrary.isBlank())
            ? GridSuiteAndConvergenceComponentLibrary.NAME
            : componentLibrary;
    }

    public void setSubstationLayout(String substationLayout) {
        this.substationLayout = (substationLayout == null || substationLayout.isBlank())
            ? DiagramConstants.SUBSTATION_LAYOUT_HORIZONTAL
            : substationLayout;
    }

    public void setSldDisplayMode(SldDisplayMode sldDisplayMode) {
        this.sldDisplayMode = (sldDisplayMode == null)
            ? SldDisplayMode.STATE_VARIABLE
            : sldDisplayMode;
    }
}
