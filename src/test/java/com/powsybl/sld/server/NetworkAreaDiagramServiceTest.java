/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.sld.server.dto.nad.NadConfigInfos;
import com.powsybl.sld.server.dto.nad.NadVoltageLevelPositionInfos;
import com.powsybl.sld.server.repository.NadConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@SpringBootTest
@Tag("IntegrationTest")
class NetworkAreaDiagramServiceTest {

    private static final UUID NONEXISTANT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Autowired
    private NadConfigRepository nadConfigRepository;

    @Autowired
    private NetworkAreaDiagramService networkAreaDiagramService;

    @BeforeEach
    void setUp() {
        nadConfigRepository.deleteAll();
    }

    NadConfigInfos createNadConfigDto() {

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
                .voltageLevelIds(List.of("VL1"))
                .depth(1)
                .radiusFactor(100)
                .scalingFactor(300000)
                .positions(positions)
                .build();
    }

    @Test
    void testReadNadConfigNotFound() {
        assertThrows(ResponseStatusException.class, () -> networkAreaDiagramService.getNetworkAreaDiagramConfig(NONEXISTANT_UUID), HttpStatus.NOT_FOUND.toString());
    }

    @Test
    void testCreateAndReadNadConfig() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());

        NadConfigInfos nadConfigDto = networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);

        assertEquals(1, nadConfigDto.getDepth());
        assertEquals(1, nadConfigDto.getVoltageLevelIds().size());
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

    @Test
    void testUpdateNadConfigNotFound() {
        NadConfigInfos configInfos = new NadConfigInfos();
        assertThrows(ResponseStatusException.class, () -> networkAreaDiagramService.updateNetworkAreaDiagramConfig(NONEXISTANT_UUID, configInfos), HttpStatus.NOT_FOUND.toString());
    }

    @Test
    void testUpdateNadConfig() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());

        // Test before update
        NadConfigInfos nadConfigDtoPreUpdate = networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);
        assertEquals(1, nadConfigDtoPreUpdate.getDepth());
        assertEquals(1, nadConfigDtoPreUpdate.getVoltageLevelIds().size());
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
        nadConfigUpdate.setVoltageLevelIds(List.of("VL1", "VL2"));

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
        assertEquals(2, nadConfigDtoPostUpdate.getVoltageLevelIds().size());
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

    @Test
    void testUpdateNadConfigMissingIds() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());
        NadConfigInfos nadConfigUpdate = new NadConfigInfos();

        // Test that the update fails if we send a position without ID or VoltageLevelId
        NadVoltageLevelPositionInfos newPositionNoIdNoVoltageLevelId = new NadVoltageLevelPositionInfos();
        newPositionNoIdNoVoltageLevelId.setXPosition(14.6);
        nadConfigUpdate.setPositions(List.of(newPositionNoIdNoVoltageLevelId));
        assertThrows(IllegalArgumentException.class, () -> networkAreaDiagramService.updateNetworkAreaDiagramConfig(nadConfigId, nadConfigUpdate), "Missing id or voltageLevelId");

        // Test that the update fails if we send a position with an ID that do not exist for this nad config
        NadVoltageLevelPositionInfos newPositionUnknownId = new NadVoltageLevelPositionInfos();
        newPositionUnknownId.setId(NONEXISTANT_UUID);
        newPositionUnknownId.setXPosition(25.2);
        nadConfigUpdate.setPositions(List.of(newPositionUnknownId));
        assertThrows(IllegalArgumentException.class, () -> networkAreaDiagramService.updateNetworkAreaDiagramConfig(nadConfigId, nadConfigUpdate), "Missing id or voltageLevelId");
    }

    @Test
    void testUpdateNadConfigVoltageLevelIdsUniqueness() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());

        NadConfigInfos nadConfigUpdate = new NadConfigInfos();

        // Test that we do not add new positions if multiple instances of the same voltage level ID are sent
        NadVoltageLevelPositionInfos newPositionA = new NadVoltageLevelPositionInfos();
        newPositionA.setVoltageLevelId("VL1");
        newPositionA.setXPosition(0.1111);

        NadVoltageLevelPositionInfos newPositionB = new NadVoltageLevelPositionInfos();
        newPositionB.setVoltageLevelId("VL1");
        newPositionB.setXPosition(0.2222);

        NadVoltageLevelPositionInfos newPositionC = new NadVoltageLevelPositionInfos();
        newPositionC.setVoltageLevelId("VL1");
        newPositionC.setXPosition(0.3333);

        nadConfigUpdate.setPositions(List.of(newPositionA, newPositionB, newPositionC));

        networkAreaDiagramService.updateNetworkAreaDiagramConfig(nadConfigId, nadConfigUpdate);

        // Test after update
        NadConfigInfos nadConfigDtoPostUpdate = networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);
        assertEquals(2, nadConfigDtoPostUpdate.getPositions().size());
        Optional<NadVoltageLevelPositionInfos> vl1PositionPostUpdate = nadConfigDtoPostUpdate.getPositions().stream()
                .filter(pos -> "VL1".equals(pos.getVoltageLevelId()))
                .findFirst();
        assertTrue(vl1PositionPostUpdate.isPresent());

        assertEquals(0.3333, vl1PositionPostUpdate.get().getXPosition(), 0.001); // Should have been updated with the last instance of the new positions
    }

    @Test
    void testUpdateNadConfigBadVoltageLevelIds() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());
        NadConfigInfos nadConfigDtoPreUpdate = networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);
        Optional<NadVoltageLevelPositionInfos> vl2Position = nadConfigDtoPreUpdate.getPositions().stream()
                .filter(pos -> "VL2".equals(pos.getVoltageLevelId()))
                .findFirst();
        assertTrue(vl2Position.isPresent());
        UUID vl2PositionUuid = vl2Position.get().getId();

        NadConfigInfos nadConfigUpdate = new NadConfigInfos();

        // Test to check if we prevent a position from adopting the voltage level ID of another existing position.
        NadVoltageLevelPositionInfos newPositionExistingIdAnotherVlId = new NadVoltageLevelPositionInfos();
        newPositionExistingIdAnotherVlId.setId(vl2PositionUuid);
        newPositionExistingIdAnotherVlId.setVoltageLevelId("VL1");
        newPositionExistingIdAnotherVlId.setXPosition(5.8);
        nadConfigUpdate.setPositions(List.of(newPositionExistingIdAnotherVlId));

        networkAreaDiagramService.updateNetworkAreaDiagramConfig(nadConfigId, nadConfigUpdate);

        // Test after update
        NadConfigInfos nadConfigDtoPostUpdate = networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);
        assertEquals(2, nadConfigDtoPostUpdate.getPositions().size());
        Optional<NadVoltageLevelPositionInfos> vl2PositionPostUpdate = nadConfigDtoPostUpdate.getPositions().stream()
                .filter(pos -> "VL2".equals(pos.getVoltageLevelId()))
                .findFirst();
        assertTrue(vl2PositionPostUpdate.isPresent());
        Optional<NadVoltageLevelPositionInfos> vl1PositionPostUpdate = nadConfigDtoPostUpdate.getPositions().stream()
                .filter(pos -> "VL1".equals(pos.getVoltageLevelId()))
                .findFirst();
        assertTrue(vl1PositionPostUpdate.isPresent());

        assertEquals(5.8, vl1PositionPostUpdate.get().getXPosition(), 0.001); // Should have been updated
        assertEquals(2.0, vl2PositionPostUpdate.get().getXPosition(), 0.001); // Should not have changed
    }

    @Test
    void testDeleteNadConfig() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());
        networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);
        networkAreaDiagramService.deleteNetworkAreaDiagramConfig(nadConfigId);

        assertThrows(ResponseStatusException.class, () -> networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId), HttpStatus.NOT_FOUND.toString());
    }
}
