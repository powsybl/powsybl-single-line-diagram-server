/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.repository;

import com.powsybl.sld.server.entities.nad.NadConfigEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@Repository
public interface NadConfigRepository extends JpaRepository<NadConfigEntity, UUID> {
    @EntityGraph(attributePaths = {"voltageLevelIds"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<NadConfigEntity> findWithVoltageLevelIdsById(UUID id);
}
