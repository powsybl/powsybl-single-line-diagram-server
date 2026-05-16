/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.sld.server.dto.nad.NadVoltageLevelPositionInfos;
import com.powsybl.sld.server.entities.nad.NadVoltageLevelConfiguredPositionEntity;
import com.powsybl.sld.server.repository.NadVoltageLevelConfiguredPositionRepository;
import com.powsybl.ws.commons.error.BaseExceptionHandler;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.ResourceUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Radouane Khouadri <redouane.khouadri_externe at rte-france.com>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BaseExceptionHandler.class)
class SupervisionControllerTest {

    public static final String SUPERVISION_CONFIG_POSITIONS_URL = "/v1/supervision/network-area-diagram/config/positions";
    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private NadVoltageLevelConfiguredPositionRepository nadVoltageLevelConfiguredPositionRepository;
    private List<NadVoltageLevelPositionInfos> positions;

    @BeforeEach
    void setUp() {

        positions = new ArrayList<>();
        when(nadVoltageLevelConfiguredPositionRepository.count()).thenAnswer(invocation -> (long) positions.size());
        doAnswer(invocation -> {
            positions.clear();
            return null;
        }).when(nadVoltageLevelConfiguredPositionRepository).deleteAll();

        doAnswer(invocation -> {
            List<NadVoltageLevelConfiguredPositionEntity> entities = invocation.getArgument(0);
            positions.addAll(entities.stream()
                    .map(NadVoltageLevelConfiguredPositionEntity::toDto)
                    .toList());
            return entities;
        }).when(nadVoltageLevelConfiguredPositionRepository).saveAll(anyList());
    }

    @Test
    void testCreatePositionsFromCsv() throws Exception {

        byte[] voltageLevelBytes = IOUtils.toByteArray(new FileInputStream(ResourceUtils.getFile("classpath:voltage-level-positions.csv")));
        MockMultipartFile file = new MockMultipartFile("file", "vl-positions.csv", "text/csv", voltageLevelBytes);
        mvc.perform(MockMvcRequestBuilders.multipart(SUPERVISION_CONFIG_POSITIONS_URL)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    void testCreatePositionsFromInvalidCsvContentType() throws Exception {
        byte[] voltageLevelBytes = IOUtils.toByteArray(new FileInputStream(ResourceUtils.getFile("classpath:voltage-level-positions.csv")));
        MockMultipartFile file = new MockMultipartFile("file", "vl-positions.csv", "invalidContentType", voltageLevelBytes);
        mvc.perform(MockMvcRequestBuilders.multipart(SUPERVISION_CONFIG_POSITIONS_URL)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    Throwable ex = result.getResolvedException();
                    assertNotNull(ex);
                    assertEquals("Invalid CSV format for NAD configured positions", ((ResponseStatusException) ex).getReason());
                });
    }

    @Test
    void testCreatePositionsFromInvalidCsvHeader() throws Exception {
        byte[] voltageLevelBytes = IOUtils.toByteArray(new FileInputStream(ResourceUtils.getFile("classpath:voltage-level-positions-invalid-header.csv")));
        MockMultipartFile file = new MockMultipartFile("file", "vl-positions.csv", "text/csv", voltageLevelBytes);
        mvc.perform(MockMvcRequestBuilders.multipart(SUPERVISION_CONFIG_POSITIONS_URL)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    Throwable ex = result.getResolvedException();
                    assertNotNull(ex);
                    assertEquals("The csv file headers are invalid for NAD configured positions", ((ResponseStatusException) ex).getReason());
                });
    }

    @Test
    void testCreatePositionsFromEmptyCsv() throws Exception {
        byte[] voltageLevelBytes = IOUtils.toByteArray(new FileInputStream(ResourceUtils.getFile("classpath:voltage-level-positions-empty.csv")));
        MockMultipartFile file = new MockMultipartFile("file", "vl-positions.csv", "text/csv", voltageLevelBytes);
        mvc.perform(MockMvcRequestBuilders.multipart(SUPERVISION_CONFIG_POSITIONS_URL)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    Throwable ex = result.getResolvedException();
                    assertNotNull(ex);
                    assertEquals("No NAD configured positions found from the csv file", ((ResponseStatusException) ex).getReason());
                });
    }

    @Test
    void testCreatingPositionsFromCsvMultipleTimes() throws Exception {

        byte[] voltageLevelBytes = IOUtils.toByteArray(new FileInputStream(ResourceUtils.getFile("classpath:voltage-level-positions.csv")));
        MockMultipartFile file = new MockMultipartFile("file", "vl-positions.csv", "text/csv", voltageLevelBytes);
        int expectedRowCount = 5;
        mvc.perform(MockMvcRequestBuilders.multipart(SUPERVISION_CONFIG_POSITIONS_URL)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());

        // Verify the mock repository methods were called as expected
        verify(nadVoltageLevelConfiguredPositionRepository, times(1)).deleteAll();
        verify(nadVoltageLevelConfiguredPositionRepository, times(1)).saveAll(anyList());

        // Verify the number of rows after the first call
        var actualRowCount = nadVoltageLevelConfiguredPositionRepository.count();
        assertEquals(expectedRowCount, actualRowCount);

        // Verify the number of rows after the second call.
        // It should still be the same as the first call because the table is cleared.
        mvc.perform(MockMvcRequestBuilders.multipart(SUPERVISION_CONFIG_POSITIONS_URL)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());
        actualRowCount = nadVoltageLevelConfiguredPositionRepository.count();
        assertEquals(expectedRowCount, actualRowCount);
    }
}
