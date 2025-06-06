/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.sld.server.dto.IdentifiableAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class FilterServiceTest {
    private static final String BASE_URI = "http://filter-server/";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FilterService filterService;

    @Mock
    private NetworkStoreService networkStoreService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        filterService.setFilterServerBaseUri(BASE_URI);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void testExportFilter() {
        UUID networkUuid = UUID.randomUUID();
        UUID filterUuid = UUID.randomUUID();
        String variantId = "variantA";

        List<IdentifiableAttributes> expectedFilterContent = List.of(new IdentifiableAttributes("vlFr1A", IdentifiableType.VOLTAGE_LEVEL, null));
        ResponseEntity<List<IdentifiableAttributes>> responseEntity = new ResponseEntity<>(expectedFilterContent, HttpStatus.OK);

        when(restTemplate.exchange(
                ArgumentMatchers.contains(filterUuid.toString()),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<List<IdentifiableAttributes>>>any())
        ).thenReturn(responseEntity);

        List<IdentifiableAttributes> result = filterService.exportFilter(networkUuid, variantId, filterUuid);

        assertEquals(expectedFilterContent, result);
    }

    @Test
    void testExportFilterNotFound() {
        UUID networkUuid = UUID.randomUUID();
        UUID filterUuid = UUID.randomUUID();
        String variantId = "variantA";

        when(restTemplate.exchange(
                ArgumentMatchers.contains(filterUuid.toString()),
                ArgumentMatchers.eq(HttpMethod.GET),
                ArgumentMatchers.isNull(),
                ArgumentMatchers.<ParameterizedTypeReference<List<IdentifiableAttributes>>>any())
        ).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertThrows(ResponseStatusException.class, () -> filterService.exportFilter(networkUuid, variantId, filterUuid));
    }
}
