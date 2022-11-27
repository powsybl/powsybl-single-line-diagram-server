/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.network.store.iidm.impl.NetworkFactoryImpl;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.ConvergenceComponentLibrary;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@RunWith(SpringRunner.class)
@WebMvcTest(SingleLineDiagramController.class)
@ContextConfiguration(classes = {SingleLineDiagramApplication.class})
public class SingleLineDiagramTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PositionDiagramLabelProvider positionDiagramLabelProvider;

    @MockBean
    private  NetworkStoreService networkStoreService;

    private static final String VARIANT_1_ID = "variant_1";
    private static final String VARIANT_2_ID = "variant_2";
    private static final String VARIANT_NOT_FOUND_ID = "variant_notFound";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void test() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        UUID notFoundNetworkId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(createNetwork());
        given(networkStoreService.getNetwork(notFoundNetworkId, null)).willThrow(new PowsyblException());

        MvcResult result = mvc.perform(get("/v1/svg/{networkUuid}/{voltageLevelId}/", testNetworkId, "vlFr1A"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
                .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        result = mvc.perform(get("/v1/svg/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_1_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
            .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        result = mvc.perform(get("/v1/svg/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_2_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
            .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        //voltage level not existing
        mvc.perform(get("/v1/svg/{networkUuid}/{voltageLevelId}/", testNetworkId, "notFound"))
                .andExpect(status().isNotFound());

        //network not existing
        mvc.perform(get("/v1/svg/{networkUuid}/{voltageLevelId}/", notFoundNetworkId, "vlFr1A"))
                .andExpect(status().isNotFound());

        //variant not existing
        mvc.perform(get("/v1/svg/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/v1/metadata/{networkUuid}/{voltageLevelId}/", testNetworkId, "vlFr1A"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(get("/v1/metadata/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_1_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        //voltage level not existing
        mvc.perform(get("/v1/metadata/{networkUuid}/{voltageLevelId}/", testNetworkId, "NotFound"))
                .andExpect(status().isNotFound());

        //network not existing
        mvc.perform(get("/v1/metadata/{networkUuid}/{voltageLevelId}/", notFoundNetworkId, "vlFr1A"))
                .andExpect(status().isNotFound());

        //variant not existing
        mvc.perform(get("/v1/metadata/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}/", testNetworkId, "vlFr1A"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_2_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        //voltage level not existing
        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}/", testNetworkId, "NotFound"))
                .andExpect(status().isNotFound());

        //network not existing
        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}/", notFoundNetworkId, "vlFr1A"))
                .andExpect(status().isNotFound());

        //variant not existing
        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testSubstations() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        UUID notFoundNetworkId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(createNetwork());
        given(networkStoreService.getNetwork(notFoundNetworkId, null)).willThrow(new PowsyblException());

        MvcResult result = mvc.perform(get("/v1/substation-svg/{networkUuid}/{substationId}/", testNetworkId, "subFr1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
                .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        result = mvc.perform(get("/v1/substation-svg/{networkUuid}/{substationId}?variantId=" + VARIANT_1_ID, testNetworkId, "subFr1"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
            .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        // substation not existing
        mvc.perform(get("/v1/substation-svg/{networkUuid}/{substationId}/", testNetworkId, "notFound"))
                .andExpect(status().isNotFound());

        // network not existing
        mvc.perform(get("/v1/substation-svg/{networkUuid}/{substationId}/", notFoundNetworkId, "subFr1"))
                .andExpect(status().isNotFound());

        // variant not existing
        mvc.perform(get("/v1/substation-svg/{networkUuid}/{substationId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "subFr1"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/v1/substation-metadata/{networkUuid}/{substationId}/", testNetworkId, "subFr1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(get("/v1/substation-metadata/{networkUuid}/{substationId}?variantId=" + VARIANT_2_ID, testNetworkId, "subFr1"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // substation not existing
        mvc.perform(get("/v1/substation-metadata/{networkUuid}/{substationId}/", testNetworkId, "NotFound"))
                .andExpect(status().isNotFound());

        // network not existing
        mvc.perform(get("/v1/substation-metadata/{networkUuid}/{substationId}/", notFoundNetworkId, "subFr2"))
                .andExpect(status().isNotFound());

        // variant not existing
        mvc.perform(get("/v1/substation-metadata/{networkUuid}/{substationId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "subFr2"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?substationLayout=horizontal", testNetworkId, "subFr2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(get("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?substationLayout=horizontal", testNetworkId, "subFr2"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(get("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?variantId=" + VARIANT_2_ID + "&substationLayout=vertical", testNetworkId, "subFr2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(get("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?topologicalColoring=true&substationLayout=horizontal", testNetworkId, "subFr2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // substation not existing
        mvc.perform(get("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}/", testNetworkId, "NotFound"))
                .andExpect(status().isNotFound());

        // network not existing
        mvc.perform(get("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}/", notFoundNetworkId, "subFr2"))
                .andExpect(status().isNotFound());

        // variant not existing
        mvc.perform(get("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "subFr2"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testComponentLibraries() throws Exception {
        MvcResult result = mvc.perform(get("/v1/svg-component-libraries"))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("[\"GridSuiteAndConvergence\",\"Convergence\"]", result.getResponse().getContentAsString());
    }

    @Test
    public void testNetworkAreaDiagram() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        UUID notFoundNetworkId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        given(networkStoreService.getNetwork(notFoundNetworkId, PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());

        MvcResult result = mvc.perform(get("/v1/network-area-diagram/{networkUuid}?variantId=" + VARIANT_2_ID + "&depth=0" + "&voltageLevelsIds=vlFr1A", testNetworkId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
                .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        result = mvc.perform(get("/v1/network-area-diagram/{networkUuid}?variantId=" + VARIANT_2_ID + "&depth=2" + "&voltageLevelsIds=vlFr1A", testNetworkId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
                .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        mvc.perform(get("/v1/network-area-diagram/{networkUuid}?variantId=" + VARIANT_2_ID + "&depth=2" + "&voltageLevelsIds=notFound", testNetworkId))
                .andExpect(status().isNotFound());
    }

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

        Substation substationEs1 = network.newSubstation()
                .setId("subEs1")
                .setCountry(Country.ES)
                .add();
        VoltageLevel voltageLevelEs1A = substationEs1.newVoltageLevel()
                .setId("vlEs1A")
                .setName("vlEs1A")
                .setNominalV(440.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelEs1A.getBusBreakerView().newBus()
                .setId("busEs1A")
                .setName("busEs1A")
                .add();
        VoltageLevel voltageLevelEs1B = substationEs1.newVoltageLevel()
                .setId("vlEs1B")
                .setName("vlEs1B")
                .setNominalV(440.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelEs1B.getBusBreakerView().newBus()
                .setId("busEs1B")
                .setName("busEs1B")
                .add();

        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_1_ID);
        network.getVariantManager().cloneVariant(VariantManagerConstants.INITIAL_VARIANT_ID, VARIANT_2_ID);

        return network;
    }

    @Test
    public void testPosisionDiagramLabelProvider() throws IOException {
        var testNetwork = new NetworkFactoryImpl().createNetwork("testNetwork", "test");
        var s1 = testNetwork.newSubstation().setId("s1").setName("s1").setCountry(Country.FR).add();
        s1.newVoltageLevel().setId("v1").setName("v1").setTopologyKind(TopologyKind.NODE_BREAKER).setNominalV(380.).add();
        var layoutParameters = new LayoutParameters();
        var componentLibrary = new ConvergenceComponentLibrary();
        var diagramLabelProvider = new PositionDiagramLabelProvider(testNetwork, componentLibrary, layoutParameters, "v1");
        var diagramStyleProvider = new NominalVoltageDiagramStyleProvider(testNetwork);
        SingleLineDiagram.draw(testNetwork, "v1", Path.of("/tmp/test.svg"), layoutParameters, componentLibrary, diagramLabelProvider, diagramStyleProvider, "");
        var line = new BufferedReader(new FileReader("/tmp/test.svg")).readLine();
        assertNotNull(line);
    }
}
