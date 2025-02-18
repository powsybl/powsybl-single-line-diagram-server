/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.repository.nad;

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
public class NadVoltageLevelPositionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "voltageLevelId")
    private String voltageLevelId;

    @Column(name = "xPosition")
    private double xPosition;

    @Column(name = "yPosition")
    private double yPosition;

    @Column(name = "xLabelPosition")
    private double xLabelPosition;

    @Column(name = "yLabelPosition")
    private double yLabelPosition;

    @ManyToOne
    @JoinColumn(name = "nad_config_id", nullable = false)
    private NadConfigEntity nadConfig;
}
