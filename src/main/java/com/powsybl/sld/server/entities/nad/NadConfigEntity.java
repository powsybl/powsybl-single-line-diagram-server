/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.entities.nad;

import com.powsybl.sld.server.dto.nad.NadConfigInfos;
import com.powsybl.sld.server.dto.nad.NadVoltageLevelPositionInfos;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "nadConfig")
public class NadConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "depth")
    private Integer depth;

    @Column(name = "scalingFactor")
    private Integer scalingFactor;

    @Column(name = "radiusFactor")
    private Integer radiusFactor;

    @OneToMany(mappedBy = "nadConfig", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NadVoltageLevelPositionEntity> positions = new ArrayList<>();

    public NadConfigInfos toDto() {
        NadConfigInfos.NadConfigInfosBuilder nadConfigInfosBuilder = NadConfigInfos.builder();
        nadConfigInfosBuilder.id(this.id)
                .depth(this.depth)
                .scalingFactor(this.scalingFactor)
                .radiusFactor(this.radiusFactor)
                .build();
        nadConfigInfosBuilder.positions(this.positions.stream().map(NadVoltageLevelPositionEntity::toDto).toList());
        return nadConfigInfosBuilder.build();
    }

    public void update(@NonNull NadConfigInfos nadConfigInfos) {
        Optional.ofNullable(nadConfigInfos.getDepth()).ifPresent(this::setDepth);
        Optional.ofNullable(nadConfigInfos.getScalingFactor()).ifPresent(this::setScalingFactor);
        Optional.ofNullable(nadConfigInfos.getRadiusFactor()).ifPresent(this::setRadiusFactor);

        if (nadConfigInfos.getPositions() != null && !nadConfigInfos.getPositions().isEmpty()) {
            Map<UUID, NadVoltageLevelPositionEntity> existingPositionsMap = this.positions.stream()
                    .collect(Collectors.toMap(NadVoltageLevelPositionEntity::getId, Function.identity()));

            for (NadVoltageLevelPositionInfos nadVoltageLevelPositionInfos : nadConfigInfos.getPositions()) {
                if (nadVoltageLevelPositionInfos.getId() != null && existingPositionsMap.containsKey(nadVoltageLevelPositionInfos.getId())) {
                    NadVoltageLevelPositionEntity existingPositionEntity = existingPositionsMap.get(nadVoltageLevelPositionInfos.getId());
                    existingPositionEntity.update(nadVoltageLevelPositionInfos);
                } else {
                    NadVoltageLevelPositionEntity newPositionEntity = nadVoltageLevelPositionInfos.toEntity();
                    newPositionEntity.setNadConfig(this);
                    this.positions.add(newPositionEntity);
                }
            }
        }
    }
}
