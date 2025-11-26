/**
 * Copyright (c) 2025 RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
/**
 * @author Kevin LE SAULNIER <kevin.le-saulnier at rte-france.com>
 */

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SvgGenerationMetadata {
    List<CurrentLimitViolationInfos> currentLimitViolationInfos;
    Map<String, Double> busIdToIccValues;

    public SvgGenerationMetadata(List<CurrentLimitViolationInfos> currentLimitViolationInfos) {
        this.currentLimitViolationInfos = currentLimitViolationInfos;
    }
}
