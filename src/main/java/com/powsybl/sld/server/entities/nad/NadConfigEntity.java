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
            updatePositions(nadConfigInfos);
        }
    }

    private void updatePositions(@NonNull NadConfigInfos nadConfigInfos) {
        // Build two lookup maps in a single iteration for better performance.
        Map<UUID, NadVoltageLevelPositionEntity> uuidPositionsMap = new HashMap<>();
        Map<String, NadVoltageLevelPositionEntity> voltageLevelIdPositionsMap = new HashMap<>();
        for (NadVoltageLevelPositionEntity position : this.positions) {
            if (position.getId() != null) {
                uuidPositionsMap.put(position.getId(), position);
            }
            if (position.getVoltageLevelId() != null) {
                voltageLevelIdPositionsMap.put(position.getVoltageLevelId(), position);
            }
        }

        for (NadVoltageLevelPositionInfos info : nadConfigInfos.getPositions()) {
            // If we have an ID, we update the corresponding position.
            // If we do not have an ID, we check if the VoltageLevelId exists, and if it does, we update the corresponding position.
            // Otherwise, we add a new position.
            if (info.getId() != null && uuidPositionsMap.containsKey(info.getId())) {
                uuidPositionsMap.get(info.getId()).update(info);
            } else if (info.getVoltageLevelId() != null && voltageLevelIdPositionsMap.containsKey(info.getVoltageLevelId())) {
                voltageLevelIdPositionsMap.get(info.getVoltageLevelId()).update(info);
            } else {
                NadVoltageLevelPositionEntity newPosition = info.toEntity();
                newPosition.setNadConfig(this);
                this.positions.add(newPosition);
            }
        }
    }
}
