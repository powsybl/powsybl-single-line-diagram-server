/*
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
public class NadConfigEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @ElementCollection
    @CollectionTable(name = "nadConfigVoltageLevel")
    private List<String> voltageLevelIds;

    @Column(name = "depth")
    private Integer depth;

    @Column(name = "scalingFactor")
    private Integer scalingFactor;

    @Column(name = "radiusFactor")
    private Integer radiusFactor;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "nad_config_id")
    @Builder.Default
    private List<NadVoltageLevelPositionEntity> positions = new ArrayList<>();

    public NadConfigInfos toDto() {
        NadConfigInfos.NadConfigInfosBuilder nadConfigInfosBuilder = NadConfigInfos.builder();
        nadConfigInfosBuilder.id(this.id)
                .voltageLevelIds(this.voltageLevelIds)
                .depth(this.depth)
                .scalingFactor(this.scalingFactor)
                .radiusFactor(this.radiusFactor)
                .build();
        nadConfigInfosBuilder.positions(this.positions.stream().map(NadVoltageLevelPositionEntity::toDto).toList());
        return nadConfigInfosBuilder.build();
    }
}
