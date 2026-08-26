package com.powsybl.sld.server;

/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.iidm.network.Country;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.sld.server.dto.Coordinate;
import com.powsybl.sld.server.dto.SubstationGeoData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeoDataServiceTest {
    private static final String BASE_URI = "http://geo-data-server/";

    private GeoDataService geoDataService;

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
        geoDataService = new GeoDataService(BASE_URI, restClientBuilder.build());
        geoDataService.setGeoDataServerBaseUri(BASE_URI);
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void testGetSubstationsGraphics() throws JsonProcessingException {
        UUID networkUuid = UUID.randomUUID();
        String variantId = "variant2";
        List<String> substationsIds = List.of("subFr1", "subFr2");

        String expectedResponse = "Substations graphics data";
        String path = BASE_URI + "v1/substations/infos?networkUuid=" + networkUuid + "&variantId=" + variantId;

        mockServer.expect(requestTo(path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json(objectMapper.writeValueAsString(substationsIds)))
                .andRespond(withSuccess(expectedResponse, MediaType.TEXT_PLAIN));

        String response = geoDataService.getSubstationsGraphics(networkUuid, variantId, substationsIds);

        assertEquals(expectedResponse, response);
    }

    @Test
    void testGetSubstationsGraphicsWithoutVariantId() {
        UUID networkUuid = UUID.randomUUID();
        List<String> substationsIds = List.of("subFr1");
        SubstationGeoData substationGeoData = new SubstationGeoData();
        substationGeoData.setId("subFr1");
        substationGeoData.setCoordinate(new Coordinate(48.8588443, 2.2943506));
        substationGeoData.setCountry(Country.FR);
        String expectedResponse = substationGeoData.toString();
        String path = BASE_URI + "v1/substations/infos?networkUuid=" + networkUuid;

        mockServer.expect(requestTo(path))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("[\"subFr1\"]"))
                .andRespond(withSuccess(expectedResponse, MediaType.TEXT_PLAIN));

        String response = geoDataService.getSubstationsGraphics(networkUuid, null, substationsIds);

        assertEquals(expectedResponse, response);
    }
}
