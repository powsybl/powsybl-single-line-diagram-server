/**
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static com.powsybl.sld.server.TestUtils.assertRequestsCount;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@SpringBootTest
class DiagramConfigRepositoryTest {

    @Autowired
    private NadConfigRepository nadConfigRepository;

    @BeforeEach
    void setUp() {
        SQLStatementCountValidator.reset();
    }

    @AfterEach
    void cleanUp() {
        nadConfigRepository.deleteAll();
    }

    NadConfigEntity createNadConfigEntity() {
        return NadConfigEntity.builder()
                .voltageLevelIds(List.of("VL1", "VL2"))
                .depth(0)
                .scalingFactor(0)
                .radiusFactor(0.0)
                .positions(
                        List.of(
                                NadVoltageLevelPositionEntity.builder()
                                        .voltageLevelId("VL1")
                                        .xPosition(0.0)
                                        .yPosition(0.0)
                                        .xLabelPosition(0.0)
                                        .yLabelPosition(0.0)
                                        .build(),
                                NadVoltageLevelPositionEntity.builder()
                                        .voltageLevelId("VL2")
                                        .xPosition(0.0)
                                        .yPosition(0.0)
                                        .xLabelPosition(0.0)
                                        .yLabelPosition(0.0)
                                        .build())
                )
                .build();
    }

    @Test
    void testCreateNadConfigQueryCount() {
        nadConfigRepository.save(createNadConfigEntity());
        assertRequestsCount(0, 5, 2, 0);
    }

    @Test
    void testDeleteNadConfigQueryCount() {
        NadConfigEntity entity = createNadConfigEntity();
        nadConfigRepository.save(entity);

        SQLStatementCountValidator.reset();
        nadConfigRepository.delete(entity);
        assertRequestsCount(5, 0, 1, 4);
    }
}
