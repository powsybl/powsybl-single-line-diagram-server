package com.powsybl.sld.server;

/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.SubstationPosition;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.server.dto.Coordinate;
import com.powsybl.sld.server.dto.SubstationGeoData;
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
import static org.junit.Assert.assertThrows;
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
    public void testGetSubstationsGraphics() {
        UUID networkUuid = UUID.randomUUID();
        String variantId = "variant2";
        List<String> substationsIds = List.of("subFr1", "subFr2");

        String expectedResponse = "Substations graphics data";
        when(restTemplate.getForObject(ArgumentMatchers.anyString(), ArgumentMatchers.eq(String.class)))
                .thenReturn(expectedResponse);

        String response = geoDataService.getSubstationsGraphics(networkUuid, variantId, substationsIds);

        assertEquals(expectedResponse, response);
    }

    @Test
    public void testGetSubstationsGraphicsWithoutVariantId() {
        UUID networkUuid = UUID.randomUUID();
        List<String> substationsIds = List.of("subFr1");
        SubstationGeoData substationGeoData = new SubstationGeoData();
        substationGeoData.setId("subFr1");
        substationGeoData.setCoordinate(new Coordinate(48.8588443, 2.2943506));
        substationGeoData.setCountry(Country.FR);
        String expectedResponse = substationGeoData.toString();
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

        String faultSubstationGeoDataJson = "[{\"id\":\"subFr1\",\"coordinate\":{\"lat\":48.8588443,\"long\":2.2943506}}]";
        when(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_1_ID, null)).thenReturn(faultSubstationGeoDataJson);
        PowsyblException exception = assertThrows(PowsyblException.class, () -> {
            geoDataService.assignSubstationGeoData(network, testNetworkId, VARIANT_1_ID, List.of(network.getSubstation("subFr2")));
        });

        // Assert the exception message
        assertEquals("Failed to parse JSON response", exception.getMessage());
        assertEquals(network.getSubstation("subFr2").getExtension(SubstationPosition.class), null);
    }

}
