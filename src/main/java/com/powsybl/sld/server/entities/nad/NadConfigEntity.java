/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.entities.nad;

import com.powsybl.sld.server.dto.nad.NadConfigInfos;
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
public class NadConfigEntity extends AbstractManuallyAssignedIdentifierEntity<UUID> {

    public NadConfigEntity(NadConfigEntity origin) {
        this.scalingFactor = origin.getScalingFactor();
        this.voltageLevelIds = new HashSet<>();
        this.voltageLevelIds.addAll(origin.getVoltageLevelIds());
        this.positions = new ArrayList<>();
        origin.getPositions().forEach(position -> this.positions.add(new NadVoltageLevelPositionEntity(position)));
    }

    @Id
    @Builder.Default
    @Column(name = "id")
    private UUID id = UUID.randomUUID();

    @ElementCollection
    @CollectionTable(name = "nadConfigVoltageLevel",
            joinColumns = @JoinColumn(name = "nad_config_entity_id"),
            indexes = @Index(name = "nad_config_voltage_level_index", columnList = "nad_config_entity_id")
    )
    @Builder.Default
    private Set<String> voltageLevelIds = new HashSet<>();

    @Column(name = "scalingFactor")
    private Integer scalingFactor;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "nad_config_id")
    @Builder.Default
    private List<NadVoltageLevelPositionEntity> positions = new ArrayList<>();

    public NadConfigInfos toDto() {
        NadConfigInfos.NadConfigInfosBuilder nadConfigInfosBuilder = NadConfigInfos.builder();
        nadConfigInfosBuilder.id(this.id)
                .voltageLevelIds(this.voltageLevelIds)
                .scalingFactor(this.scalingFactor)
                .build();
        nadConfigInfosBuilder.positions(this.positions.stream().map(NadVoltageLevelPositionEntity::toDto).toList());
        return nadConfigInfosBuilder.build();
    }
}
