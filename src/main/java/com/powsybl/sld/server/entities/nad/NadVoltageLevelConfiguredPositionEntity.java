/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.entities.nad;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
@Entity
@Table(name = "nadVoltageLevelConfiguredPosition")
public class NadVoltageLevelConfiguredPositionEntity extends AbstractNadVoltageLevelPositionEntity {
}
