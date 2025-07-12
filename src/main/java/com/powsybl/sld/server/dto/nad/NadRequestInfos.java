/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto.nad;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class NadRequestInfos {
    private UUID nadConfigUuid;
    private UUID filterUuid;
    @Builder.Default
    private List<String> voltageLevelIds = new ArrayList<>();
    @Builder.Default
    private List<String> voltageLevelToExpandIds = new ArrayList<>();
    @Builder.Default
    private List<String> voltageLevelToOmitIds = new ArrayList<>();
    @Builder.Default
    private List<NadVoltageLevelPositionInfos> positions = new ArrayList<>();
    @Builder.Default
    private Boolean withGeoData = true;
}
