package com.powsybl.sld.server;

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.LinePosition;
import com.powsybl.iidm.network.extensions.SubstationPosition;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.server.dto.LineGeoData;
import com.powsybl.sld.server.utils.GeoDataUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

public class GeoDataServiceTest {

    private static final String VARIANT_1_ID = "variant_1";
    private static final String VARIANT_2_ID = "variant_2";
    private final String baseUri = "http://geo-data-server/";
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private GeoDataService geoDataService;
    @Mock
    private NetworkStoreService networkStoreService;

    public static Network createNetwork() {
        Network network = Network.create("test", "test");
        Substation substationFr1 = network.newSubstation()
                .setId("subFr1")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();
        VoltageLevel voltageLevelFr1A = substationFr1.newVoltageLevel()
                .setId("vlFr1A")
                .setName("vlFr1A")
                .setNominalV(440.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelFr1A.getBusBreakerView().newBus()
                .setId("busFr1A")
                .setName("busFr1A")
                .add();
        VoltageLevel voltageLevelFr1B = substationFr1.newVoltageLevel()
                .setId("vlFr1B").setName("vlFr1B")
                .setNominalV(200.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelFr1B.getBusBreakerView().newBus()
                .setId("busFr1B")
                .setName("busFr1B")
                .add();

        Substation substationFr2 = network.newSubstation()
                .setId("subFr2")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();
        VoltageLevel voltageLevelFr2A = substationFr2.newVoltageLevel()
                .setId("vlFr2A")
                .setName("vlFr2A")
                .setNominalV(440.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelFr2A.getBusBreakerView().newBus()
                .setId("busFr2A")
                .setName("busFr2A")
                .add();
        Line lineFr1 = network.newLine()
                .setId("lineFr1")
                .setVoltageLevel1("vlFr2A")
                .setBus1("busFr2A")
                .setConnectableBus1("busFr2A")
                .setVoltageLevel2("vlFr2A")
                .setBus2("busFr2A")
                .setConnectableBus2("busFr2A")
                .setR(3.0)
                .setX(33.0)
                .setG1(0.0)
                .setB1(386E-6 / 2)
                .setG2(0.0)
                .setB2(386E-6 / 2)
                .add();
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_2_ID);

        return network;
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
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

    @Test
    public void testAssignSubstationGeoData() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        Network network = networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION);
        String substationGeoDataJson = "[{\"id\":\"subFr1\",\"coordinate\":{\"lat\":48.8588443,\"lon\":2.2943506}},{\"id\":\"subFr2\",\"coordinate\":{\"lat\":51.507351,\"lon\":1.127758}}]";
        when(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_1_ID, null)).thenReturn(substationGeoDataJson);

        geoDataService.assignSubstationGeoData(network, testNetworkId, VARIANT_1_ID, List.of(network.getSubstation("subFr1")));
        assertEquals(network.getSubstation("subFr1").getExtension(SubstationPosition.class).getCoordinate(), new com.powsybl.iidm.network.extensions.Coordinate(48.8588443, 2.2943506));
        assertEquals(network.getSubstation("subFr2").getExtension(SubstationPosition.class), null);
    }

    @Test
    public void testAssignLineGeoData() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        Network network = networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION);
        String lineGeoDataJson = "[{\"id\":\"lineFr1\",\"coordinates\":[{\"lat\":48.8588443,\"lon\":2.2943506},{\"lat\":48.8588444,\"lon\":2.2943507}]},{\"id\":\"lineFr2\",\"coordinates\":[{\"lat\":51.507351,\"lon\":-0.127758},{\"lat\":51.507352,\"lon\":-0.127759}]}]";
        when(geoDataService.getLinesGraphics(testNetworkId, VARIANT_1_ID, null)).thenReturn(lineGeoDataJson);

        List<LineGeoData> linesGeoData = GeoDataUtils.fromStringToLineGeoData(lineGeoDataJson, new ObjectMapper());
        Map<String, List<com.powsybl.sld.server.dto.Coordinate>> lineGeoDataMap = linesGeoData.stream()
                .collect(Collectors.toMap(LineGeoData::getId, LineGeoData::getCoordinates));

        // Convert Iterable to List
        List<Line> lines = StreamSupport.stream(network.getLines().spliterator(), false)
                .collect(Collectors.toList());

        geoDataService.assignLineGeoData(network, testNetworkId, VARIANT_1_ID, lines);
        assertNotNull("Line position will not be null", network.getLine("lineFr1").getExtension(LinePosition.class));
    }
}
