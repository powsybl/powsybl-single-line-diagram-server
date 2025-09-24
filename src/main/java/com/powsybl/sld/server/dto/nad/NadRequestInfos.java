/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto.nad;

import com.powsybl.sld.server.dto.CurrentLimitViolationInfos;
import com.powsybl.sld.server.utils.NadPositionsGenerationMode;
import lombok.*;

import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class NadRequestInfos {
    private UUID nadConfigUuid;
    private UUID filterUuid;
    @Builder.Default
    private Set<String> voltageLevelIds = new HashSet<>();
    @Builder.Default
    private Set<String> voltageLevelToExpandIds = new HashSet<>();
    @Builder.Default
    private Set<String> voltageLevelToOmitIds = new HashSet<>();
    @Builder.Default
    private List<NadVoltageLevelPositionInfos> positions = new ArrayList<>();
    private NadPositionsGenerationMode nadPositionsGenerationMode;
    private UUID nadPositionsConfigUuid;
    private List<CurrentLimitViolationInfos> currentLimitViolationsInfos;
}
