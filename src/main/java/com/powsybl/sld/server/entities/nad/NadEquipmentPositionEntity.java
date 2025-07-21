/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.entities.nad;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Builder
@Table(name = "nadEquipmentPosition")

public class NadEquipmentPositionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;

    @Column(name = "equipmentId", nullable = false)
    private String equipmentId;

    @Column(name = "equipmentType")
    private Integer equipmentType;

    @Column(name = "xPosition")
    private Double xPosition;

    @Column(name = "yPosition")
    private Double yPosition;

    @Column(name = "xLabelPosition")
    private Double xLabelPosition;

    @Column(name = "yLabelPosition")
    private Double yLabelPosition;
}
