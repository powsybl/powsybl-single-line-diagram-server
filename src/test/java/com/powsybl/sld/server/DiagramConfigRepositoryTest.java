/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.sld.server.entities.nad.NadConfigEntity;
import com.powsybl.sld.server.entities.nad.NadVoltageLevelPositionEntity;
import com.powsybl.sld.server.repository.NadConfigRepository;
import com.vladmihalcea.sql.SQLStatementCountValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static com.powsybl.sld.server.TestUtils.assertRequestsCount;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@SpringBootTest
@Tag("IntegrationTest")
class DiagramConfigRepositoryTest {

    @Autowired
    private NadConfigRepository nadConfigRepository;

    @BeforeEach
    void setUp() {
        nadConfigRepository.deleteAll();
        SQLStatementCountValidator.reset();
    }

    @Test
    void testCreateNadConfigQueryCount() {
        NadVoltageLevelPositionEntity position1 = NadVoltageLevelPositionEntity.builder()
                .voltageLevelId("VL1")
                .xPosition(0.0)
                .yPosition(0.0)
                .xLabelPosition(0.0)
                .yLabelPosition(0.0)
                .build();

        NadVoltageLevelPositionEntity position2 = NadVoltageLevelPositionEntity.builder()
                .voltageLevelId("VL2")
                .xPosition(0.0)
                .yPosition(0.0)
                .xLabelPosition(0.0)
                .yLabelPosition(0.0)
                .build();

        ArrayList<NadVoltageLevelPositionEntity> positions = new ArrayList<>();
        positions.add(position1);
        positions.add(position2);

        NadConfigEntity entity = NadConfigEntity.builder()
                .depth(0)
                .scalingFactor(0)
                .radiusFactor(0)
                .positions(positions)
                .build();

        position1.setNadConfig(entity);
        position2.setNadConfig(entity);

        nadConfigRepository.save(entity);
        assertRequestsCount(0, 3, 0, 0);
    }

    @Test
    void testDeleteNadConfigQueryCount() {
        NadVoltageLevelPositionEntity position1 = NadVoltageLevelPositionEntity.builder()
                .voltageLevelId("VL1")
                .xPosition(0.0)
                .yPosition(0.0)
                .xLabelPosition(0.0)
                .yLabelPosition(0.0)
                .build();

        NadVoltageLevelPositionEntity position2 = NadVoltageLevelPositionEntity.builder()
                .voltageLevelId("VL2")
                .xPosition(0.0)
                .yPosition(0.0)
                .xLabelPosition(0.0)
                .yLabelPosition(0.0)
                .build();

        ArrayList<NadVoltageLevelPositionEntity> positions = new ArrayList<>();
        positions.add(position1);
        positions.add(position2);

        NadConfigEntity entity = NadConfigEntity.builder()
                .depth(0)
                .scalingFactor(0)
                .radiusFactor(0)
                .positions(positions)
                .build();

        position1.setNadConfig(entity);
        position2.setNadConfig(entity);

        nadConfigRepository.save(entity);

        nadConfigRepository.delete(entity);

        // We test that all the positions are also deleted
        assertRequestsCount(4, 3, 0, 3);
    }

}
