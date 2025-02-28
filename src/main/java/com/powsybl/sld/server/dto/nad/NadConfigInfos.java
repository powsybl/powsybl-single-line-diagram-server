/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto.nad;

import com.powsybl.sld.server.entities.nad.NadConfigEntity;
import lombok.*;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

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
    @Builder.Default
    private List<String> voltageLevelIds = new ArrayList<>();
    private Integer depth;
    private Integer scalingFactor;
    private Integer radiusFactor;
    @Builder.Default
    private List<NadVoltageLevelPositionInfos> positions = new ArrayList<>();

    public NadConfigEntity toEntity() {
        return NadConfigEntity.builder()
                .id(id)
                .voltageLevelIds(voltageLevelIds)
                .depth(depth)
                .scalingFactor(scalingFactor)
                .radiusFactor(radiusFactor)
                .positions(positions.stream().map(NadVoltageLevelPositionInfos::toEntity).toList())
                .build();
    }
}
