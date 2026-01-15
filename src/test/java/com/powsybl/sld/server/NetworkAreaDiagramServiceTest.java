/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.sld.server.dto.nad.NadConfigInfos;
import com.powsybl.sld.server.dto.nad.NadVoltageLevelPositionInfos;
import com.powsybl.sld.server.entities.nad.NadConfigEntity;
import com.powsybl.sld.server.error.DiagramBusinessException;
import com.powsybl.sld.server.repository.NadConfigRepository;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

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
                .voltageLevelIds(Set.of("VL1"))
                .scalingFactor(300000)
                .positions(positions)
                .build();
    }

    @Test
    void testReadNadConfigNotFound() {
        assertThrows(RuntimeException.class, () -> networkAreaDiagramService.getNetworkAreaDiagramConfig(NONEXISTANT_UUID), HttpStatus.NOT_FOUND.toString());
    }

    @Test
    void testCreateAndReadNadConfig() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());

        NadConfigInfos nadConfigDto = networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);

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
        assertThrows(RuntimeException.class, () -> networkAreaDiagramService.updateNetworkAreaDiagramConfig(NONEXISTANT_UUID, configInfos), HttpStatus.NOT_FOUND.toString());
    }

    @Test
    void testDuplicateNadConfig() {
        UUID originNadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());
        UUID duplicateNadConfigId = networkAreaDiagramService.duplicateNetworkAreaDiagramConfig(originNadConfigId);

        NadConfigInfos originNadConfigInfos = networkAreaDiagramService.getNetworkAreaDiagramConfig(originNadConfigId);
        NadConfigInfos duplicateNadConfigInfos = networkAreaDiagramService.getNetworkAreaDiagramConfig(duplicateNadConfigId);

        // check ids are different for duplicated entities...
        assertNotEquals(originNadConfigInfos.getId(), duplicateNadConfigInfos.getId());
        // ... and duplicated children
        assertNotEquals(originNadConfigInfos.getPositions().getFirst().getId(), duplicateNadConfigInfos.getPositions().getFirst().getId());

        // then check all field properties, except id / position.id are equal
        assertThat(duplicateNadConfigInfos).usingRecursiveComparison().ignoringFieldsMatchingRegexes(".*\\.id$|id").isEqualTo(originNadConfigInfos);
    }

    @Test
    void testDuplicateNadConfigNotFound() {
        assertThrows(RuntimeException.class, () -> networkAreaDiagramService.duplicateNetworkAreaDiagramConfig(UUID.randomUUID()), HttpStatus.NOT_FOUND.toString());
        assertEquals(0, nadConfigRepository.count());
    }

    @Test
    void testUpdateNadConfig() {
        UUID nadConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());

        // Test before update
        NadConfigInfos nadConfigDtoPreUpdate = networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId);
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
        nadConfigUpdate.setScalingFactor(600000);
        nadConfigUpdate.setVoltageLevelIds(Set.of("VL1", "VL2"));

        ArrayList<NadVoltageLevelPositionInfos> positions = new ArrayList<>();
        NadVoltageLevelPositionInfos newPositionVl3 = new NadVoltageLevelPositionInfos();
        newPositionVl3.setVoltageLevelId("VL3");
        newPositionVl3.setXPosition(3.33);
        newPositionVl3.setYPosition(3.66);
        newPositionVl3.setXLabelPosition(3.99);
        newPositionVl3.setYLabelPosition(3.11);

        NadVoltageLevelPositionInfos updatedVl2 = new NadVoltageLevelPositionInfos();
        updatedVl2.setVoltageLevelId("VL2");
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
        assertThrows(DiagramBusinessException.class, () -> networkAreaDiagramService.updateNetworkAreaDiagramConfig(nadConfigId, nadConfigUpdate), "Missing id or voltageLevelId");

        // Test that the update fails if we send a position with an ID that do not exist for this nad config
        NadVoltageLevelPositionInfos newPositionUnknownId = new NadVoltageLevelPositionInfos();
        newPositionUnknownId.setId(NONEXISTANT_UUID);
        newPositionUnknownId.setXPosition(25.2);
        nadConfigUpdate.setPositions(List.of(newPositionUnknownId));
        assertThrows(DiagramBusinessException.class, () -> networkAreaDiagramService.updateNetworkAreaDiagramConfig(nadConfigId, nadConfigUpdate), "Missing id or voltageLevelId");
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

        assertThrows(RuntimeException.class, () -> networkAreaDiagramService.getNetworkAreaDiagramConfig(nadConfigId), HttpStatus.NOT_FOUND.toString());
    }

    @Test
    @Transactional
    void testCreateMultipleNetworkAreaDiagramConfigs() {
        // Create multiple configs
        NadConfigInfos config1 = createNadConfigDto();

        NadConfigInfos config2 = createNadConfigDto();
        config2.setVoltageLevelIds(Set.of("VL2", "VL3"));

        NadConfigInfos config3 = createNadConfigDto();
        config3.setVoltageLevelIds(Set.of("VL4"));

        List<NadConfigInfos> configs = List.of(config1, config2, config3);

        // Verify repository is empty before creation
        assertEquals(0, nadConfigRepository.count());

        // Create multiple configs
        networkAreaDiagramService.createNetworkAreaDiagramConfigs(configs);

        // Verify all configs were created
        assertEquals(3, nadConfigRepository.count());

        // Verify each config was saved correctly
        List<NadConfigInfos> savedConfigs = nadConfigRepository.findAll().stream()
            .map(NadConfigEntity::toDto)
            .toList();

        assertEquals(3, savedConfigs.size());

        // Check that the configs have different IDs (UUIDs were generated or preserved)
        Set<UUID> configIds = savedConfigs.stream()
            .map(NadConfigInfos::getId)
            .collect(java.util.stream.Collectors.toSet());
        assertEquals(3, configIds.size()); // All IDs should be unique
    }

    @Test
    void testCreateMultipleNetworkAreaDiagramConfigsWithProvidedUUIDs() {
        // Test that provided UUIDs are preserved
        UUID predefinedId1 = UUID.randomUUID();
        UUID predefinedId2 = UUID.randomUUID();

        NadConfigInfos config1 = createNadConfigDto();
        config1.setId(predefinedId1);

        NadConfigInfos config2 = createNadConfigDto();
        config2.setId(predefinedId2);

        List<NadConfigInfos> configs = List.of(config1, config2);

        networkAreaDiagramService.createNetworkAreaDiagramConfigs(configs);

        // Verify configs were saved with the provided UUIDs
        NadConfigInfos savedConfig1 = networkAreaDiagramService.getNetworkAreaDiagramConfig(predefinedId1);
        NadConfigInfos savedConfig2 = networkAreaDiagramService.getNetworkAreaDiagramConfig(predefinedId2);

        assertEquals(predefinedId1, savedConfig1.getId());
        assertEquals(predefinedId2, savedConfig2.getId());
    }

    @Test
    void testDeleteMultipleNetworkAreaDiagramConfigs() {
        // Create multiple configs first
        UUID config1Id = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());
        UUID config2Id = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());
        UUID config3Id = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());

        // Verify all configs exist
        assertEquals(3, nadConfigRepository.count());
        assertDoesNotThrow(() -> networkAreaDiagramService.getNetworkAreaDiagramConfig(config1Id));
        assertDoesNotThrow(() -> networkAreaDiagramService.getNetworkAreaDiagramConfig(config2Id));
        assertDoesNotThrow(() -> networkAreaDiagramService.getNetworkAreaDiagramConfig(config3Id));

        // Delete multiple configs
        List<UUID> configsToDelete = List.of(config1Id, config3Id);
        networkAreaDiagramService.deleteNetworkAreaDiagramConfigs(configsToDelete);

        // Verify only config2 remains
        assertEquals(1, nadConfigRepository.count());
        assertThrows(RuntimeException.class, () -> networkAreaDiagramService.getNetworkAreaDiagramConfig(config1Id));
        assertDoesNotThrow(() -> networkAreaDiagramService.getNetworkAreaDiagramConfig(config2Id));
        assertThrows(RuntimeException.class, () -> networkAreaDiagramService.getNetworkAreaDiagramConfig(config3Id));
    }

    @Test
    void testDeleteMultipleNetworkAreaDiagramConfigsEmptyList() {
        // Create some configs first
        networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());
        networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());

        assertEquals(2, nadConfigRepository.count());

        // Delete with empty list should not affect anything
        networkAreaDiagramService.deleteNetworkAreaDiagramConfigs(List.of());

        // Verify no configs were deleted
        assertEquals(2, nadConfigRepository.count());
    }

    @Test
    void testDeleteMultipleNetworkAreaDiagramConfigsNonExistentIds() {
        // Create one config
        UUID existingConfigId = networkAreaDiagramService.createNetworkAreaDiagramConfig(createNadConfigDto());
        assertEquals(1, nadConfigRepository.count());

        // Try to delete a mix of existing and non-existing IDs
        UUID nonExistentId1 = UUID.randomUUID();
        UUID nonExistentId2 = UUID.randomUUID();

        List<UUID> configsToDelete = List.of(existingConfigId, nonExistentId1, nonExistentId2);

        // This should not throw an exception and should delete the existing config
        assertDoesNotThrow(() -> networkAreaDiagramService.deleteNetworkAreaDiagramConfigs(configsToDelete));

        // Verify the existing config was deleted
        assertEquals(0, nadConfigRepository.count());
        assertThrows(RuntimeException.class, () -> networkAreaDiagramService.getNetworkAreaDiagramConfig(existingConfigId));
    }
}
