/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto.nad;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.powsybl.sld.server.entities.nad.NadVoltageLevelPositionEntity;
import lombok.*;

import java.util.UUID;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class NadVoltageLevelPositionInfos {
    private UUID id;
    private String voltageLevelId;
    // As this attribute has only one lower case letter at its start (xXXXX), the getters is parsed as getXPosition and the field for Jackson is parsed as xposition
    // while we expect xPosition. JsonProperty let fix the json field to xPosition
    @JsonProperty("xPosition")
    private Double xPosition;
    @JsonProperty("yPosition")
    private Double yPosition;
    @JsonProperty("xLabelPosition")
    private Double xLabelPosition;
    @JsonProperty("yLabelPosition")
    private Double yLabelPosition;

    public NadVoltageLevelPositionEntity toEntity() {
        return NadVoltageLevelPositionEntity.builder()
                .id(id)
                .voltageLevelId(voltageLevelId)
                .xPosition(xPosition)
                .yPosition(yPosition)
                .xLabelPosition(xLabelPosition)
                .yLabelPosition(yLabelPosition)
                .build();
    }
}
