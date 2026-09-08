/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.iidm.network.IdentifiableType;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.sld.server.dto.IdentifiableAttributes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FilterServiceTest {
    private static final String BASE_URI = "http://filter-server/";

    private FilterService filterService;

    @Mock
    private NetworkStoreService networkStoreService;

    private MockRestServiceServer mockServer;
    private AutoCloseable mocks;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        filterService = new FilterService(BASE_URI, restClientBuilder.build());
        filterService.setFilterServerBaseUri(BASE_URI);
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void testExportFilter() throws JsonProcessingException {
        UUID networkUuid = UUID.randomUUID();
        UUID filterUuid = UUID.randomUUID();
        String variantId = "variantA";

        List<IdentifiableAttributes> expectedFilterContent = List.of(new IdentifiableAttributes("vlFr1A", IdentifiableType.VOLTAGE_LEVEL, null));
        String path = BASE_URI + "v1/filters/" + filterUuid + "/export?networkUuid=" + networkUuid + "&variantId=" + variantId;

        mockServer.expect(requestTo(path))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(objectMapper.writeValueAsString(expectedFilterContent), MediaType.APPLICATION_JSON));

        List<IdentifiableAttributes> result = filterService.exportFilter(networkUuid, variantId, filterUuid);

        assertThat(result.getFirst()).usingRecursiveComparison().isEqualTo(expectedFilterContent.getFirst());
    }

    @Test
    void testExportFilterNotFound() {
        UUID networkUuid = UUID.randomUUID();
        UUID filterUuid = UUID.randomUUID();
        String variantId = "variantA";
        String path = BASE_URI + "v1/filters/" + filterUuid + "/export?networkUuid=" + networkUuid + "&variantId=" + variantId;

        mockServer.expect(requestTo(path))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(HttpClientErrorException.NotFound.class, () -> filterService.exportFilter(networkUuid, variantId, filterUuid));
    }
}
