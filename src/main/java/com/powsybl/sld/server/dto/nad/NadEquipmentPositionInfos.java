/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.sld.server.dto.nad;

import com.powsybl.sld.server.entities.nad.NadEquipmentPositionEntity;
import lombok.*;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class NadEquipmentPositionInfos {

    private String equipmentId;
    private Integer equipmentType;
    private Double xPosition;
    private Double yPosition;
    private Double xLabelPosition;
    private Double yLabelPosition;

    public NadEquipmentPositionEntity toEntity() {
        return NadEquipmentPositionEntity.builder()
                .equipmentId(equipmentId)
                .equipmentType(equipmentType)
                .xPosition(xPosition)
                .yPosition(yPosition)
                .xLabelPosition(xLabelPosition)
                .yLabelPosition(yLabelPosition)
                .build();
    }
}
