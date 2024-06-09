package com.powsybl.sld.server;
/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class GeoDataServiceTest {

    private final String baseUri = "http://geo-data-server/";
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private GeoDataService geoDataService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        geoDataService.setGeoDataServerBaseUri(baseUri);
    }

    @Test
    public void testGetLinesGraphics() {
        UUID networkUuid = UUID.randomUUID();
        String variantId = "variant1";
        List<String> linesIds = List.of("line1", "line2");

        String expectedResponse = "Lines graphics data";
        when(restTemplate.getForObject(ArgumentMatchers.anyString(), ArgumentMatchers.eq(String.class)))
                .thenReturn(expectedResponse);

        String response = geoDataService.getLinesGraphics(networkUuid, variantId, linesIds);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testGetSubstationsGraphics() {
        UUID networkUuid = UUID.randomUUID();
        String variantId = "variant2";
        List<String> substationsIds = List.of("substation1", "substation2");

        String expectedResponse = "Substations graphics data";
        when(restTemplate.getForObject(ArgumentMatchers.anyString(), ArgumentMatchers.eq(String.class)))
                .thenReturn(expectedResponse);

        String response = geoDataService.getSubstationsGraphics(networkUuid, variantId, substationsIds);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testGetLinesGraphicsWithoutVariantId() {
        UUID networkUuid = UUID.randomUUID();
        List<String> linesIds = List.of("line1", "line2");

        String expectedResponse = "Lines graphics data without variant";
        when(restTemplate.getForObject(ArgumentMatchers.anyString(), ArgumentMatchers.eq(String.class)))
                .thenReturn(expectedResponse);

        String response = geoDataService.getLinesGraphics(networkUuid, null, linesIds);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testGetSubstationsGraphicsWithoutVariantId() {
        UUID networkUuid = UUID.randomUUID();
        List<String> substationsIds = List.of("substation1", "substation2");

        String expectedResponse = "Substations graphics data without variant";
        when(restTemplate.getForObject(ArgumentMatchers.anyString(), ArgumentMatchers.eq(String.class)))
                .thenReturn(expectedResponse);

        String response = geoDataService.getSubstationsGraphics(networkUuid, null, substationsIds);

        assertEquals(expectedResponse, response);
    }
}