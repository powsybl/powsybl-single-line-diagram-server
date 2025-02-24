/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto.nad;

import com.powsybl.sld.server.entities.nad.NadConfigEntity;
import com.powsybl.sld.server.entities.nad.NadVoltageLevelPositionEntity;
import lombok.*;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class NadConfigInfos {
    private UUID id;
    private Integer depth;
    private Integer scalingFactor;
    private Integer radiusFactor;
    private List<NadVoltageLevelPositionInfos> positions;

    public NadConfigEntity toNadConfigEntity() {
        NadConfigEntity nadConfigEntity = NadConfigEntity.builder()
                .id(id)
                .depth(depth)
                .scalingFactor(scalingFactor)
                .radiusFactor(radiusFactor)
                .build();
        List<NadVoltageLevelPositionEntity> positionsEntities = this.positions.stream()
                .map(position -> {
                    NadVoltageLevelPositionEntity entity = position.toEntity();
                    entity.setNadConfig(nadConfigEntity);
                    return entity;
                })
                .collect(Collectors.toCollection(ArrayList::new));

        nadConfigEntity.setPositions(positionsEntities);
        return nadConfigEntity;
    }
}
