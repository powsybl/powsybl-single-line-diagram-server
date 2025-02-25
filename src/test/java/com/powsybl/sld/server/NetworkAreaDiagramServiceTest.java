/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.sld.server.dto.nad.NadConfigInfos;
import com.powsybl.sld.server.dto.nad.NadVoltageLevelPositionInfos;
import com.powsybl.sld.server.entities.nad.NadConfigEntity;
import com.powsybl.sld.server.entities.nad.NadVoltageLevelPositionEntity;
import com.powsybl.sld.server.repository.NadConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@SpringBootTest
@Tag("IntegrationTest")
public class NetworkAreaDiagramServiceTest {

    @Autowired
    private NadConfigRepository nadConfigRepository;

    @Autowired
    private NetworkAreaDiagramService networkAreaDiagramService;

    @BeforeEach
    void setUp() {
        nadConfigRepository.deleteAll();
    }

    public NadConfigInfos createNadConfigDto() {

        NadVoltageLevelPositionInfos vlPositionInfos1 = NadVoltageLevelPositionInfos.builder()
                .voltageLevelId("VL1")
                .xPosition(1.0)
                .yPosition(1.1)
                .xLabelPosition(1.2)
                .yLabelPosition(1.3)
                .build();

        NadVoltageLevelPositionInfos vlPositionInfos2 = NadVoltageLevelPositionInfos.builder()
                .voltageLevelId("VL2")
                .xPosition(2.0)
                .yPosition(2.1)
                .xLabelPosition(2.2)
                .yLabelPosition(2.3)
                .build();

        ArrayList<NadVoltageLevelPositionInfos> positions = new ArrayList<>();
        positions.add(vlPositionInfos1);
        positions.add(vlPositionInfos2);

        return NadConfigInfos.builder()
                .depth(1)
                .radiusFactor(100)
                .scalingFactor(300000)
                .positions(positions)
                .build();
    }

    @Transactional
    @Test
    void testCreateNadConfig() {
        UUID newNadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());

        Optional<NadConfigEntity> createdConfig = nadConfigRepository.findById(newNadConfigId);

        assertTrue(createdConfig.isPresent());
        assertEquals(1, createdConfig.get().getDepth());
        assertEquals(2, createdConfig.get().getPositions().size());

        // Check position
        Optional<NadVoltageLevelPositionEntity> vl2Position = createdConfig.get().getPositions().stream()
                .filter(pos -> "VL2".equals(pos.getVoltageLevelId()))
                .findFirst();
        assertTrue(vl2Position.isPresent());
        assertEquals(2.0, vl2Position.get().getXPosition(), 0.001);
    }

    @Transactional
    @Test
    void testReadNadConfigNotFound() {
        try {
            networkAreaDiagramService.getNetworkAreaDiagramConfig(UUID.randomUUID());
            fail();
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Transactional
    @Test
    void testReadNadConfig() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());

        NadConfigInfos nadConfigDto = networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);

        assertEquals(1, nadConfigDto.getDepth());
        assertEquals(2, nadConfigDto.getPositions().size());

        // Check position
        Optional<NadVoltageLevelPositionInfos> vl1Position = nadConfigDto.getPositions().stream()
                .filter(pos -> "VL1".equals(pos.getVoltageLevelId()))
                .findFirst();
        assertTrue(vl1Position.isPresent());
        assertEquals(1.0, vl1Position.get().getXPosition(), 0.001);
        assertEquals(1.1, vl1Position.get().getYPosition(), 0.001);
        assertEquals(1.2, vl1Position.get().getXLabelPosition(), 0.001);
        assertEquals(1.3, vl1Position.get().getYLabelPosition(), 0.001);
    }

    @Transactional
    @Test
    void testUpdateNadConfigNotFound() {
        try {
            networkAreaDiagramService.updateNetworkAreaDiagramConfig(UUID.randomUUID(), new NadConfigInfos());
            fail();
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }

    @Transactional
    @Test
    void testUpdateNadConfig() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());

        // Test before update
        NadConfigInfos nadConfigDtoPreUpdate = networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);
        assertEquals(1, nadConfigDtoPreUpdate.getDepth());
        assertEquals(300000, nadConfigDtoPreUpdate.getScalingFactor());
        assertEquals(2, nadConfigDtoPreUpdate.getPositions().size());
        Optional<NadVoltageLevelPositionInfos> vl2Position = nadConfigDtoPreUpdate.getPositions().stream()
                .filter(pos -> "VL2".equals(pos.getVoltageLevelId()))
                .findFirst();
        assertTrue(vl2Position.isPresent());
        assertEquals(2.0, vl2Position.get().getXPosition(), 0.001);

        // Update
        NadConfigInfos nadConfigUpdate = new NadConfigInfos();
        nadConfigUpdate.setDepth(18);
        nadConfigUpdate.setScalingFactor(600000);

        ArrayList<NadVoltageLevelPositionInfos> positions = new ArrayList<>();
        NadVoltageLevelPositionInfos newPositionVl3 = new NadVoltageLevelPositionInfos();
        newPositionVl3.setVoltageLevelId("VL3");
        newPositionVl3.setXPosition(3.33);
        newPositionVl3.setYPosition(3.66);
        newPositionVl3.setXLabelPosition(3.99);
        newPositionVl3.setYLabelPosition(3.11);

        NadVoltageLevelPositionInfos updatedVl2 = new NadVoltageLevelPositionInfos();
        updatedVl2.setId(vl2Position.get().getId());
        updatedVl2.setXPosition(65.0);

        NadVoltageLevelPositionInfos updatedVl1 = new NadVoltageLevelPositionInfos();
        updatedVl1.setVoltageLevelId("VL1");
        updatedVl1.setXPosition(111.111);

        positions.add(newPositionVl3);
        positions.add(updatedVl2);
        positions.add(updatedVl1);
        nadConfigUpdate.setPositions(positions);

        networkAreaDiagramService.updateNetworkAreaDiagramConfig(nadConfigId, nadConfigUpdate);

        // Test after update
        NadConfigInfos nadConfigDtoPostUpdate = networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);
        assertEquals(18, nadConfigDtoPostUpdate.getDepth());
        assertEquals(600000, nadConfigDtoPostUpdate.getScalingFactor());
        assertEquals(3, nadConfigDtoPostUpdate.getPositions().size());

        Optional<NadVoltageLevelPositionInfos> vl2PositionPostUpdate = nadConfigDtoPostUpdate.getPositions().stream()
                .filter(pos -> "VL2".equals(pos.getVoltageLevelId()))
                .findFirst();
        assertTrue(vl2PositionPostUpdate.isPresent());
        assertEquals(65.0, vl2PositionPostUpdate.get().getXPosition(), 0.001);

        Optional<NadVoltageLevelPositionInfos> vl3PositionPostUpdate = nadConfigDtoPostUpdate.getPositions().stream()
                .filter(pos -> "VL3".equals(pos.getVoltageLevelId()))
                .findFirst();
        assertTrue(vl3PositionPostUpdate.isPresent());
        assertEquals(3.33, vl3PositionPostUpdate.get().getXPosition(), 0.001);

        Optional<NadVoltageLevelPositionInfos> vl1PositionPostUpdate = nadConfigDtoPostUpdate.getPositions().stream()
                .filter(pos -> "VL1".equals(pos.getVoltageLevelId()))
                .findFirst();
        assertTrue(vl1PositionPostUpdate.isPresent());
        assertEquals(111.111, vl1PositionPostUpdate.get().getXPosition(), 0.001);
    }

    @Transactional
    @Test
    void testDeleteNadConfig() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());
        networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);
        networkAreaDiagramService.deleteNetworkAreaDiagramConfig(nadConfigId);
        try {
            networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);
            fail();
        } catch (ResponseStatusException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }
    }
}
