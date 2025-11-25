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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class SldRequestInfos {
    private List<CurrentLimitViolationInfos> currentLimitViolationsInfos;
    private List<BaseVoltageConfig> baseVoltagesConfigInfos;
    Map<String, Double> busIdToIccValues;

    public SldRequestInfos(List<CurrentLimitViolationInfos> currentLimitViolationsInfos) {
        this.currentLimitViolationsInfos = currentLimitViolationsInfos;
    }
}
