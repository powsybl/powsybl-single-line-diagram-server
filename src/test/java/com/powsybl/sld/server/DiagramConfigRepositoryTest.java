/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.sld.server.entities.nad.NadConfigEntity;
import com.powsybl.sld.server.repository.NadConfigRepository;
import com.vladmihalcea.sql.SQLStatementCountValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.sld.server.TestUtils.assertRequestsCount;
import static com.vladmihalcea.sql.SQLStatementCountValidator.assertInsertCount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@SpringBootTest
public class DiagramConfigRepositoryTest {
    private static final UUID TEST_CONFIG_ID = UUID.fromString("bfa68792-222d-40e1-8703-670ea22b493a");

    @Autowired
    private NadConfigRepository nadConfigRepository;

    @BeforeEach
    void setUp() {
        nadConfigRepository.deleteAll();
        SQLStatementCountValidator.reset();
    }

    @Test
    void testCreateNadConfigQueryCount() {
        NadConfigEntity entity = NadConfigEntity.builder()
                        .id(TEST_CONFIG_ID)
                .depth(0)
                .scalingFactor(0)
                .radiusFactor(0)
                .positions(Collections.emptyList())
                .build();

        nadConfigRepository.save(entity);

        // No select
        assertInsertCount(1);
        //assertRequestsCount(0, 1, 0, 0);
        assertThat(nadConfigRepository.findById(TEST_CONFIG_ID)).isPresent();

    }

}
