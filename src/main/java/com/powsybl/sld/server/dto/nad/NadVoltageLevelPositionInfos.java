/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto.nad;

import com.powsybl.sld.server.entities.nad.NadVoltageLevelPositionEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@Builder
@Getter
@Setter
public class NadVoltageLevelPositionInfos {
    private UUID id;
    private String voltageLevelId;
    private Double xPosition;
    private Double yPosition;
    private Double xLabelPosition;
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
