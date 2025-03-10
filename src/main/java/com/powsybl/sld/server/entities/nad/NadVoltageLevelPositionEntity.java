/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.entities.nad;

import com.powsybl.sld.server.dto.nad.NadVoltageLevelPositionInfos;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "nadVoltageLevelPosition", indexes = {
    @Index(name = "nadConfigEntity_positions_index", columnList = "nad_config_id")
})
public class NadVoltageLevelPositionEntity {

    public NadVoltageLevelPositionEntity(NadVoltageLevelPositionEntity origin) {
        this.voltageLevelId = origin.getVoltageLevelId();
        this.xPosition = origin.getXPosition();
        this.yPosition = origin.getYPosition();
        this.xLabelPosition = origin.getXLabelPosition();
        this.yLabelPosition = origin.getYLabelPosition();
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "voltageLevelId", nullable = false)
    private String voltageLevelId;

    @Column(name = "xPosition")
    private Double xPosition;

    @Column(name = "yPosition")
    private Double yPosition;

    @Column(name = "xLabelPosition")
    private Double xLabelPosition;

    @Column(name = "yLabelPosition")
    private Double yLabelPosition;

    public NadVoltageLevelPositionInfos toDto() {
        return NadVoltageLevelPositionInfos.builder()
                .id(id)
                .voltageLevelId(voltageLevelId)
                .xPosition(xPosition)
                .yPosition(yPosition)
                .xLabelPosition(xLabelPosition)
                .yLabelPosition(yLabelPosition)
                .build();
    }
}
