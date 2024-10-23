/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.extensions.Extendable;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.BusbarSectionPositionAdder;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.iidm.network.extensions.ConnectablePositionAdder;
import com.powsybl.iidm.network.extensions.SubstationPosition;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.SldParameters;
import com.powsybl.sld.builders.NetworkGraphBuilder;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.ConvergenceComponentLibrary;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.server.dto.SvgAndMetadata;
import com.powsybl.sld.server.utils.SingleLineDiagramParameters;
import com.powsybl.sld.server.utils.SldDisplayMode;
import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.SvgParameters;
import com.powsybl.sld.svg.styles.NominalVoltageStyleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static com.powsybl.sld.library.ComponentTypeName.ARROW_ACTIVE;
import static com.powsybl.sld.library.ComponentTypeName.ARROW_REACTIVE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@WebMvcTest(SingleLineDiagramController.class)
@ContextConfiguration(classes = {SingleLineDiagramApplication.class})
class SingleLineDiagramTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SingleLineDiagramService singleLineDiagramService;

    @Autowired
    private NetworkAreaDiagramService networkAreaDiagramService;

    @MockBean
    private PositionDiagramLabelProvider positionDiagramLabelProvider;

    @MockBean
    private NetworkStoreService networkStoreService;
    @MockBean
    private GeoDataService geoDataService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String VARIANT_1_ID = "variant_1";
    private static final String VARIANT_2_ID = "variant_2";
    private static final String VARIANT_NOT_FOUND_ID = "variant_notFound";
    private FileSystem fileSystem;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        Files.createDirectory(fileSystem.getPath("tmp"));
    }

    @AfterEach
    void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    void test() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        UUID notFoundNetworkId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(createNetwork());
        given(networkStoreService.getNetwork(notFoundNetworkId, null)).willThrow(new PowsyblException());

        MvcResult result = mvc.perform(get("/v1/svg/{networkUuid}/{voltageLevelId}", testNetworkId, "vlFr1A"))
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
        mvc.perform(get("/v1/svg/{networkUuid}/{voltageLevelId}", testNetworkId, "notFound"))
                .andExpect(status().isNotFound());

        //network not existing
        mvc.perform(get("/v1/svg/{networkUuid}/{voltageLevelId}", notFoundNetworkId, "vlFr1A"))
                .andExpect(status().isNotFound());

        //variant not existing
        mvc.perform(get("/v1/svg/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/v1/metadata/{networkUuid}/{voltageLevelId}", testNetworkId, "vlFr1A"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(get("/v1/metadata/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_1_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        //voltage level not existing
        mvc.perform(get("/v1/metadata/{networkUuid}/{voltageLevelId}", testNetworkId, "NotFound"))
                .andExpect(status().isNotFound());

        //network not existing
        mvc.perform(get("/v1/metadata/{networkUuid}/{voltageLevelId}", notFoundNetworkId, "vlFr1A"))
                .andExpect(status().isNotFound());

        //variant not existing
        mvc.perform(get("/v1/metadata/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}", testNetworkId, "vlFr1A"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_2_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}?sldDisplayMode=" + SldDisplayMode.FEEDER_POSITION.name() + "&variantId=" + VARIANT_2_ID, testNetworkId, "vlFr1A"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        //voltage level not existing
        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}", testNetworkId, "NotFound"))
                .andExpect(status().isNotFound());

        //network not existing
        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}", notFoundNetworkId, "vlFr1A"))
                .andExpect(status().isNotFound());

        //variant not existing
        mvc.perform(get("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testSubstations() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        UUID notFoundNetworkId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(createNetwork());
        given(networkStoreService.getNetwork(notFoundNetworkId, null)).willThrow(new PowsyblException());

        MvcResult result = mvc.perform(get("/v1/substation-svg/{networkUuid}/{substationId}", testNetworkId, "subFr1"))
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
        mvc.perform(get("/v1/substation-svg/{networkUuid}/{substationId}", testNetworkId, "notFound"))
                .andExpect(status().isNotFound());

        // network not existing
        mvc.perform(get("/v1/substation-svg/{networkUuid}/{substationId}", notFoundNetworkId, "subFr1"))
                .andExpect(status().isNotFound());

        // variant not existing
        mvc.perform(get("/v1/substation-svg/{networkUuid}/{substationId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "subFr1"))
            .andExpect(status().isNotFound());

        mvc.perform(get("/v1/substation-metadata/{networkUuid}/{substationId}", testNetworkId, "subFr1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(get("/v1/substation-metadata/{networkUuid}/{substationId}?variantId=" + VARIANT_2_ID, testNetworkId, "subFr1"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // substation not existing
        mvc.perform(get("/v1/substation-metadata/{networkUuid}/{substationId}", testNetworkId, "NotFound"))
                .andExpect(status().isNotFound());

        // network not existing
        mvc.perform(get("/v1/substation-metadata/{networkUuid}/{substationId}", notFoundNetworkId, "subFr2"))
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
        mvc.perform(get("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}", testNetworkId, "NotFound"))
                .andExpect(status().isNotFound());

        // network not existing
        mvc.perform(get("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}", notFoundNetworkId, "subFr2"))
                .andExpect(status().isNotFound());

        // variant not existing
        mvc.perform(get("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "subFr2"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testComponentLibraries() throws Exception {
        MvcResult result = mvc.perform(get("/v1/svg-component-libraries"))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("[\"GridSuiteAndConvergence\",\"Convergence\",\"FlatDesign\"]", result.getResponse().getContentAsString());
    }

    private static final String GEO_DATA_SUBSTATIONS = "/geo_data_substations.json";

    @Test
    void testAssignSubstationGeoData() {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        Network network = networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION);

        String substationGeoDataJson = "[{\"id\":\"subFr1\",\"coordinate\":{\"lat\":48.8588443,\"lon\":2.2943506}},{\"id\":\"subFr2\",\"coordinate\":{\"lat\":51.507351,\"lon\":1.127758}}]";
        given(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_1_ID, List.of("subFr2"))).willReturn(substationGeoDataJson);

        networkAreaDiagramService.assignGeoDataCoordinates(network, testNetworkId, VARIANT_1_ID, List.of(network.getVoltageLevel("vlFr2A")));
        assertEquals(network.getSubstation("subFr2").getExtension(SubstationPosition.class).getCoordinate(), new com.powsybl.iidm.network.extensions.Coordinate(51.507351, 1.127758));
        assertNull(network.getSubstation("subFr1").getExtension(SubstationPosition.class));

        String faultSubstationGeoDataJson = "[{\"id\":\"subFr1\",\"coordinate\":{\"lat\":48.8588443,\"long\":2.2943506}}]";
        given(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_1_ID, List.of("subFr1"))).willReturn(faultSubstationGeoDataJson);
        PowsyblException exception = assertThrows(PowsyblException.class, () ->
            networkAreaDiagramService.assignGeoDataCoordinates(network, testNetworkId, VARIANT_1_ID, List.of(network.getVoltageLevel("vlFr1A"))));

        // Assert the exception message
        assertEquals("Failed to parse JSON response", exception.getMessage());
        assertNull(network.getSubstation("subFr1").getExtension(SubstationPosition.class));
    }

    @Test
    void testNetworkAreaDiagram() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        UUID notFoundNetworkId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        given(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_2_ID, List.of("subFr1"))).willReturn(toString(GEO_DATA_SUBSTATIONS));
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        given(networkStoreService.getNetwork(notFoundNetworkId, PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());

        MvcResult result = mvc.perform(get("/v1/network-area-diagram/{networkUuid}?variantId=" + VARIANT_2_ID + "&depth=0" + "&voltageLevelsIds=vlFr1A", testNetworkId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();
        String stringResult = result.getResponse().getContentAsString();
        assertTrue(stringResult.contains("svg"));
        assertTrue(stringResult.contains("metadata"));
        assertTrue(stringResult.contains("additionalMetadata"));
        assertTrue(stringResult.contains("<?xml"));

        result = mvc.perform(get("/v1/network-area-diagram/{networkUuid}?variantId=" + VARIANT_2_ID + "&depth=2" + "&voltageLevelsIds=vlFr1A", testNetworkId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();
        String stringResult2 = result.getResponse().getContentAsString();
        assertTrue(stringResult2.contains("svg"));
        assertTrue(stringResult.contains("metadata"));
        assertTrue(stringResult.contains("additionalMetadata"));
        assertTrue(stringResult2.contains("<?xml"));

        mvc.perform(get("/v1/network-area-diagram/{networkUuid}?variantId=" + VARIANT_2_ID + "&depth=2" + "&voltageLevelsIds=notFound", testNetworkId))
                .andExpect(status().isNotFound());
    }

    private void testGenerateNadBasedOnGeoData(boolean withGeoData) throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        given(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_2_ID, List.of("subFr1"))).willReturn(toString(GEO_DATA_SUBSTATIONS));
        networkAreaDiagramService.generateNetworkAreaDiagramSvg(testNetworkId, VARIANT_2_ID, List.of("vlFr1A"), 0, withGeoData);
        if (withGeoData) {
            //initialize with geographical data
            verify(geoDataService, times(1)).getSubstationsGraphics(testNetworkId, VARIANT_2_ID, List.of("subFr1"));
        } else {
            //initialize without geographical data
            verify(geoDataService, times(0)).getSubstationsGraphics(any(), any(), any());
        }
    }

    @Test
    void testGenerateNadWithoutGeoData() throws Exception {
        testGenerateNadBasedOnGeoData(false);
    }

    @Test
    void testGenerateNadWithGeoData() throws Exception {
        testGenerateNadBasedOnGeoData(true);
    }

    @Test
    void testNetworkAreaDiagramAdditionalMetadata() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");

        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        given(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_2_ID, List.of("subFr1"))).willReturn(toString(GEO_DATA_SUBSTATIONS));

        SvgAndMetadata svgAndMetadata = networkAreaDiagramService.generateNetworkAreaDiagramSvg(testNetworkId, VARIANT_2_ID, List.of("vlFr1A"), 2, true);
        Object additionalMetadata = svgAndMetadata.getAdditionalMetadata();
        assertNotNull(additionalMetadata);
        Map<String, Object> convertedMetadata = objectMapper.convertValue(additionalMetadata, new TypeReference<>() { });
        assertEquals(1, convertedMetadata.get("nbVoltageLevels"));
        List<Map<String, String>> voltageLevels = objectMapper.convertValue(convertedMetadata.get("voltageLevels"), new TypeReference<>() { });
        assertNotNull(voltageLevels);
        assertEquals(1, voltageLevels.size());
        assertEquals("vlFr1A", voltageLevels.get(0).get("id"));
        assertEquals("vlFr1A", voltageLevels.get(0).get("name"));
        assertEquals("subFr1", voltageLevels.get(0).get("substationId"));
    }

    @Test
    void testVoltageLevelSingleLineDiagramAdditionalMetadata() {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");

        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(createNetwork());

        SingleLineDiagramParameters parameters = SingleLineDiagramParameters.builder()
                .useName(false)
                .labelCentered(false)
                .diagonalLabel(false)
                .topologicalColoring(false)
                .componentLibrary(GridSuiteAndConvergenceComponentLibrary.NAME)
                .substationLayout("horizontal")
                .sldDisplayMode(SldDisplayMode.STATE_VARIABLE)
                .language("en")
                .build();

        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(testNetworkId, VARIANT_2_ID, "vlFr1A", parameters);
        Object additionalMetadata = svgAndMetadata.getAdditionalMetadata();
        assertNotNull(additionalMetadata);
        Map<String, String> convertedMetadata = objectMapper.convertValue(additionalMetadata, new TypeReference<>() { });
        assertEquals("vlFr1A", convertedMetadata.get("id"));
        assertEquals("vlFr1A", convertedMetadata.get("name"));
        assertEquals("FR", convertedMetadata.get("country"));
        assertEquals("subFr1", convertedMetadata.get("substationId"));
    }

    @Test
    void testSubstationSingleLineDiagramAdditionalMetadata() {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");

        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(createNetwork());

        SingleLineDiagramParameters parameters = SingleLineDiagramParameters.builder()
                .useName(false)
                .labelCentered(false)
                .diagonalLabel(false)
                .topologicalColoring(false)
                .componentLibrary(GridSuiteAndConvergenceComponentLibrary.NAME)
                .substationLayout("horizontal")
                .sldDisplayMode(SldDisplayMode.STATE_VARIABLE)
                .language("en")
                .build();

        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(testNetworkId, VARIANT_2_ID, "subFr1", parameters);
        Object additionalMetadata = svgAndMetadata.getAdditionalMetadata();
        assertNotNull(additionalMetadata);
        Map<String, String> convertedMetadata = objectMapper.convertValue(additionalMetadata, new TypeReference<>() { });
        assertEquals("subFr1", convertedMetadata.get("id"));
        assertNull(convertedMetadata.get("name"));
        assertEquals("FR", convertedMetadata.get("country"));
    }

    private static Network createNetwork() {
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
    void testPositionDiagramLabelProvider() throws Exception {
        var testNetwork = createNetworkWithTwoInjectionAndOneBranchAndOne3twt();
        var layoutParameters = new LayoutParameters();
        var svgParameters = new SvgParameters();
        var sldParameters = new SldParameters();
        var componentLibrary = new ConvergenceComponentLibrary();
        var graphBuilder = new NetworkGraphBuilder(testNetwork);
        VoltageLevelGraph g = graphBuilder.buildVoltageLevelGraph("vl1");
        PositionDiagramLabelProvider labelProvider = new PositionDiagramLabelProvider(testNetwork, componentLibrary, layoutParameters, svgParameters, "vl1");

        List<FeederInfo> feederInfos1 = labelProvider.getFeederInfos((FeederNode) g.getNode("loadA"));
        assertEquals(2, feederInfos1.size());
        assertEquals(ARROW_ACTIVE, feederInfos1.get(0).getComponentType());
        assertEquals(ARROW_REACTIVE, feederInfos1.get(1).getComponentType());
        assertTrue(feederInfos1.get(0).getRightLabel().isPresent());
        assertTrue(feederInfos1.get(1).getRightLabel().isPresent());
        assertFalse(feederInfos1.get(0).getLeftLabel().isPresent());
        assertFalse(feederInfos1.get(1).getLeftLabel().isPresent());
        sldParameters.setLabelProviderFactory((a, b, c, d) -> new PositionDiagramLabelProvider(testNetwork, componentLibrary, layoutParameters, svgParameters, "vl1"));
        sldParameters.setStyleProviderFactory(n -> new NominalVoltageStyleProvider());
        sldParameters.setComponentLibrary(componentLibrary);
        sldParameters.setLayoutParameters(layoutParameters);

        // test if position label successfully added to svg
        try (var svgWriter = new StringWriter();
             var metadataWriter = new StringWriter()) {
            SingleLineDiagram.draw(testNetwork, "vl1", svgWriter, metadataWriter, sldParameters);
            assertTrue(svgWriter.toString().contains("loadA (pos: 0)"));
            assertTrue(svgWriter.toString().contains("trf1 (pos: 1)"));
            assertTrue(svgWriter.toString().contains("trf73 (pos: 3)"));
            sldParameters.setLabelProviderFactory((a, b, c, d) -> new PositionDiagramLabelProvider(testNetwork, componentLibrary, layoutParameters, svgParameters, "vl2"));
            SingleLineDiagram.draw(testNetwork, "vl2", svgWriter, metadataWriter, sldParameters);
            assertTrue(svgWriter.toString().contains("trf1 (pos: 1)"));
            sldParameters.setLabelProviderFactory((a, b, c, d) -> new PositionDiagramLabelProvider(testNetwork, componentLibrary, layoutParameters, svgParameters, "vl3"));
            SingleLineDiagram.draw(testNetwork, "vl3", svgWriter, metadataWriter, sldParameters);
            assertTrue(svgWriter.toString().contains("trf71 (pos: 6)"));
        }

    }

    @Test
    void testNetworkAreaDiagramWithMissingVoltageLevel() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        given(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_2_ID, List.of("subFr1"))).willReturn(toString(GEO_DATA_SUBSTATIONS));

        SvgAndMetadata svgAndMetadata = networkAreaDiagramService.generateNetworkAreaDiagramSvg(testNetworkId, VARIANT_2_ID, List.of("vlFr1A", "vlNotFound1"), 0, true);
        Object additionalMetadata = svgAndMetadata.getAdditionalMetadata();
        assertNotNull(additionalMetadata);
        Map<String, Object> convertedMetadata = objectMapper.convertValue(additionalMetadata, new TypeReference<>() { });
        assertEquals(1, convertedMetadata.get("nbVoltageLevels"));
        List<Map<String, String>> voltageLevels = objectMapper.convertValue(convertedMetadata.get("voltageLevels"), new TypeReference<>() { });
        assertNotNull(voltageLevels);
        assertEquals(1, voltageLevels.size());
        assertEquals("vlFr1A", voltageLevels.get(0).get("id"));
        mvc.perform(get("/v1/network-area-diagram/{networkUuid}?variantId=" + VARIANT_2_ID + "&depth=0" + "&voltageLevelsIds=vlNotFound1,vlNotFound2", testNetworkId))
                .andExpect(status().isNotFound());
    }

    private static String toString(String resourceName) throws IOException {
        return new String(ByteStreams.toByteArray(Objects.requireNonNull(SingleLineDiagramTest.class.getResourceAsStream(resourceName))), StandardCharsets.UTF_8);
    }

    /*
        #TODO replace it with already configured FourSubstationsNodeBreakerWithExtensionsFactory when migrating to next powsybl release
    */
    private static Network createNetworkWithTwoInjectionAndOneBranchAndOne3twt() {
        Network network = Network.create("TestSingleLineDiagram", "test");
        Substation substation = createSubstation(network, "s", "s", Country.FR);
        Substation substation2 = createSubstation(network, "s2", "s2", Country.FR);
        VoltageLevel vl1 = createVoltageLevel(substation, "vl1", "vl1", TopologyKind.NODE_BREAKER, 380);
        VoltageLevel vl2 = createVoltageLevel(substation, "vl2", "vl2", TopologyKind.NODE_BREAKER, 225);
        VoltageLevel vl3 = createVoltageLevel(substation, "vl3", "vl3", TopologyKind.NODE_BREAKER, 225);
        VoltageLevel vl4 = createVoltageLevel(substation2, "vl4", "vl4", TopologyKind.NODE_BREAKER, 220);

        createBusBarSection(vl1, "bbs11", "bbs11", 2, 2, 2);
        createLoad(vl1, "loadA", "loadA", "loadA", 0, ConnectablePosition.Direction.TOP, 4, 10, 10);
        createSwitch(vl1, "bA", "bA", SwitchKind.BREAKER, false, false, false, 3, 4);

        createBusBarSection(vl2, "bbs22", "bbs22", 5, 5, 5);
        createSwitch(vl1, "bA1", "bA1", SwitchKind.BREAKER, false, false, false, 6, 7);
        createSwitch(vl2, "bA11", "bA11", SwitchKind.BREAKER, false, false, false, 7, 8);
        createTwoWindingsTransformer(substation, "trf1", "trf1", 2.0, 14.745, 0.0, 3.2E-5, 400.0, 225.0,
                7, 8, vl1.getId(), vl2.getId(),
                "trf1", 1, ConnectablePosition.Direction.TOP,
                "trf1", 1, ConnectablePosition.Direction.TOP);
        createBusBarSection(vl3, "bbs33", "bbs33", 8, 8, 8);
        createSwitch(vl1, "bA33", "bA33", SwitchKind.BREAKER, false, false, false, 9, 10);
        createSwitch(vl2, "bA44", "bA44", SwitchKind.BREAKER, false, false, false, 11, 12);
        createSwitch(vl3, "bA55", "bA55", SwitchKind.BREAKER, false, false, false, 13, 14);
        createThreeWindingsTransformer(substation, "trf2", "trf2", "vl1", "vl2", "vl3",
                0.5, 0.5, 0.5, 1., 1., 1., 0.1, 0.1,
                50., 225., 400.,
                10, 12, 14,
                "trf73", 3, ConnectablePosition.Direction.BOTTOM,
                "trf72", 4, ConnectablePosition.Direction.TOP,
                "trf71", 6, ConnectablePosition.Direction.BOTTOM);
        createBusBarSection(vl4, "bbs66", "bbs66", 2, 2, 2);
        createLoad(vl4, "loadB", "loadB", "loadB", null, ConnectablePosition.Direction.TOP, 4, 10, 10);
        createSwitch(vl4, "bB", "bB", SwitchKind.BREAKER, false, false, false, 3, 4);
        return network;
    }

    private static Substation createSubstation(Network n, String id, String name, Country country) {
        return n.newSubstation()
                .setId(id)
                .setName(name)
                .setCountry(country)
                .add();
    }

    private static VoltageLevel createVoltageLevel(Substation s, String id, String name, TopologyKind topology, double vNom) {
        return s.newVoltageLevel()
                .setId(id)
                .setName(name)
                .setTopologyKind(topology)
                .setNominalV(vNom)
                .add();
    }

    private static void createLoad(VoltageLevel vl, String id, String name, String feederName, Integer feederOrder,
                                   ConnectablePosition.Direction direction, int node, double p0, double q0) {
        Load load = vl.newLoad()
                .setId(id)
                .setName(name)
                .setNode(node)
                .setP0(p0)
                .setQ0(q0)
                .add();
        addFeederPosition(load, feederName, feederOrder, direction);
    }

    private static void createSwitch(VoltageLevel vl, String id, String name, SwitchKind kind, boolean retained, boolean open, boolean fictitious, int node1, int node2) {
        vl.getNodeBreakerView().newSwitch()
                .setId(id)
                .setName(name)
                .setKind(kind)
                .setRetained(retained)
                .setOpen(open)
                .setFictitious(fictitious)
                .setNode1(node1)
                .setNode2(node2)
                .add();
    }

    private static void createBusBarSection(VoltageLevel vl, String id, String name, int node, int busbarIndex, int sectionIndex) {
        BusbarSection bbs = vl.getNodeBreakerView().newBusbarSection()
                .setId(id)
                .setName(name)
                .setNode(node)
                .add();
        bbs.newExtension(BusbarSectionPositionAdder.class)
                .withBusbarIndex(busbarIndex)
                .withSectionIndex(sectionIndex)
                .add();
    }

    private static void createTwoWindingsTransformer(Substation s, String id, String name,
                                                     double r, double x, double g, double b,
                                                     double ratedU1, double ratedU2,
                                                     int node1, int node2,
                                                     String idVoltageLevel1, String idVoltageLevel2,
                                                     String feederName1, Integer feederOrder1, ConnectablePosition.Direction direction1,
                                                     String feederName2, Integer feederOrder2, ConnectablePosition.Direction direction2) {
        TwoWindingsTransformer t = s.newTwoWindingsTransformer()
                .setId(id)
                .setName(name)
                .setR(r)
                .setX(x)
                .setG(g)
                .setB(b)
                .setRatedU1(ratedU1)
                .setRatedU2(ratedU2)
                .setNode1(node1)
                .setVoltageLevel1(idVoltageLevel1)
                .setNode2(node2)
                .setVoltageLevel2(idVoltageLevel2)
                .add();
        addTwoFeedersPosition(t, feederName1, feederOrder1, direction1, feederName2, feederOrder2, direction2);
    }

    private static void createThreeWindingsTransformer(Substation s, String id, String name,
                                                       String vl1, String vl2, String vl3,
                                                       double r1, double r2, double r3,
                                                       double x1, double x2, double x3,
                                                       double g1, double b1,
                                                       double ratedU1, double ratedU2, double ratedU3,
                                                       int node1, int node2, int node3,
                                                       String feederName1, Integer feederOrder1, ConnectablePosition.Direction direction1,
                                                       String feederName2, Integer feederOrder2, ConnectablePosition.Direction direction2,
                                                       String feederName3, Integer feederOrder3, ConnectablePosition.Direction direction3) {
        ThreeWindingsTransformer t = s.newThreeWindingsTransformer()
                .setId(id)
                .setName(name)
                .newLeg1()
                .setR(r1)
                .setX(x1)
                .setG(g1)
                .setB(b1)
                .setRatedU(ratedU1)
                .setVoltageLevel(vl1)
                .setNode(node1)
                .add()
                .newLeg2()
                .setR(r2)
                .setX(x2)
                .setRatedU(ratedU2)
                .setVoltageLevel(vl2)
                .setNode(node2)
                .add()
                .newLeg3()
                .setR(r3)
                .setX(x3)
                .setRatedU(ratedU3)
                .setVoltageLevel(vl3)
                .setNode(node3)
                .add()
                .add();

        addThreeFeedersPosition(t, feederName1, feederOrder1, direction1, feederName2, feederOrder2, direction2, feederName3, feederOrder3, direction3);
    }

    private static void addTwoFeedersPosition(Extendable<?> extendable,
                                               String feederName1, Integer feederOrder1, ConnectablePosition.Direction direction1,
                                               String feederName2, Integer feederOrder2, ConnectablePosition.Direction direction2) {
        ConnectablePositionAdder extensionAdder = extendable.newExtension(ConnectablePositionAdder.class);
        ConnectablePositionAdder.FeederAdder feederAdder1 = extensionAdder.newFeeder1();
        if (feederOrder1 != null) {
            feederAdder1.withOrder(feederOrder1);
        }
        feederAdder1.withName(feederName1).withDirection(direction1).add();
        ConnectablePositionAdder.FeederAdder feederAdder2 = extensionAdder.newFeeder2();
        if (feederOrder2 != null) {
            feederAdder2.withOrder(feederOrder2);
        }
        feederAdder2.withName(feederName2).withDirection(direction2).add();
        extensionAdder.add();
    }

    private static void addThreeFeedersPosition(Extendable<?> extendable,
                                                String feederName1, Integer feederOrder1, ConnectablePosition.Direction direction1,
                                                String feederName2, Integer feederOrder2, ConnectablePosition.Direction direction2,
                                                String feederName3, Integer feederOrder3, ConnectablePosition.Direction direction3) {
        ConnectablePositionAdder extensionAdder = extendable.newExtension(ConnectablePositionAdder.class);
        ConnectablePositionAdder.FeederAdder feederAdder1 = extensionAdder.newFeeder1();
        if (feederOrder1 != null) {
            feederAdder1.withOrder(feederOrder1);
        }
        feederAdder1.withName(feederName1).withDirection(direction1).add();
        ConnectablePositionAdder.FeederAdder feederAdder2 = extensionAdder.newFeeder2();
        if (feederOrder2 != null) {
            feederAdder2.withOrder(feederOrder2);
        }
        feederAdder2.withName(feederName2).withDirection(direction2).add();
        ConnectablePositionAdder.FeederAdder feederAdder3 = extensionAdder.newFeeder3();
        if (feederOrder3 != null) {
            feederAdder3.withOrder(feederOrder3);
        }
        feederAdder3.withName(feederName3).withDirection(direction3).add();
        extensionAdder.add();
    }

    private static void addFeederPosition(Extendable<?> extendable, String feederName, Integer feederOrder, ConnectablePosition.Direction direction) {
        ConnectablePositionAdder.FeederAdder feederAdder = extendable.newExtension(ConnectablePositionAdder.class).newFeeder();
        if (feederOrder != null) {
            feederAdder.withOrder(feederOrder);
        }
        feederAdder.withDirection(direction).withName(feederName).add()
                .add();
    }
}
