package com.powsybl.sld.server;

/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.powsybl.iidm.network.Country;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.sld.server.dto.Coordinate;
import com.powsybl.sld.server.dto.SubstationGeoData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class GeoDataServiceTest {
    private static final String BASE_URI = "http://geo-data-server/";

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GeoDataService geoDataService;

    @Mock
    private NetworkStoreService networkStoreService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        geoDataService.setGeoDataServerBaseUri(BASE_URI);
    }

    @Test
    void testGetSubstationsGraphics() {
        UUID networkUuid = UUID.randomUUID();
        String variantId = "variant2";
        List<String> substationsIds = List.of("subFr1", "subFr2");

        String expectedResponse = "Substations graphics data";
        when(restTemplate.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.anyList(), ArgumentMatchers.eq(String.class)))
                .thenReturn(expectedResponse);

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
        when(restTemplate.postForObject(ArgumentMatchers.anyString(), ArgumentMatchers.anyList(), ArgumentMatchers.eq(String.class)))
                .thenReturn(expectedResponse);

        String response = geoDataService.getSubstationsGraphics(networkUuid, null, substationsIds);

        assertEquals(expectedResponse, response);
    }
}
