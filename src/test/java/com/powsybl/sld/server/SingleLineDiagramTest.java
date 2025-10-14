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
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerWithExtensionsFactory;
import com.powsybl.nad.svg.StyleProvider;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.SldParameters;
import com.powsybl.sld.builders.NetworkGraphBuilder;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.ConvergenceComponentLibrary;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.server.dto.CurrentLimitViolationInfos;
import com.powsybl.sld.server.dto.IdentifiableAttributes;
import com.powsybl.sld.server.dto.SvgAndMetadata;
import com.powsybl.sld.server.dto.nad.NadConfigInfos;
import com.powsybl.sld.server.dto.nad.NadGenerationContext;
import com.powsybl.sld.server.dto.nad.NadRequestInfos;
import com.powsybl.sld.server.dto.nad.NadVoltageLevelPositionInfos;
import com.powsybl.sld.server.entities.nad.NadVoltageLevelConfiguredPositionEntity;
import com.powsybl.sld.server.repository.NadConfigRepository;
import com.powsybl.sld.server.repository.NadVoltageLevelConfiguredPositionRepository;
import com.powsybl.sld.server.utils.NadPositionsGenerationMode;
import com.powsybl.sld.server.utils.SingleLineDiagramParameters;
import com.powsybl.sld.server.utils.SldDisplayMode;
import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.SvgParameters;
import com.powsybl.sld.svg.styles.NominalVoltageStyleProvider;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.ResourceUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.*;

import static com.powsybl.sld.library.SldComponentTypeName.ARROW_ACTIVE;
import static com.powsybl.sld.library.SldComponentTypeName.ARROW_REACTIVE;
import static com.powsybl.sld.svg.styles.StyleClassConstants.OVERLOAD_STYLE_CLASS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SingleLineDiagramTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private SingleLineDiagramService singleLineDiagramService;

    @Autowired
    private NetworkAreaDiagramService networkAreaDiagramService;

    @MockBean
    private NadConfigRepository nadConfigRepository;

    @MockBean
    private NadVoltageLevelConfiguredPositionRepository nadVoltageLevelConfiguredPositionRepository;

    @MockBean
    private PositionDiagramLabelProvider positionDiagramLabelProvider;

    @MockBean
    private NetworkStoreService networkStoreService;
    @MockBean
    private GeoDataService geoDataService;
    @MockBean
    private FilterService filterService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String VARIANT_1_ID = "variant_1";
    private static final String VARIANT_2_ID = "variant_2";
    private static final String VARIANT_NOT_FOUND_ID = "variant_notFound";
    private FileSystem fileSystem;
    private List<NadVoltageLevelPositionInfos> positions;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        Files.createDirectory(fileSystem.getPath("tmp"));

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

        MvcResult result = mvc.perform(post("/v1/svg/{networkUuid}/{voltageLevelId}", testNetworkId, "vlFr1A"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
                .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        result = mvc.perform(post("/v1/svg/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_1_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
            .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        result = mvc.perform(post("/v1/svg/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_2_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
            .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        //voltage level not existing
        mvc.perform(post("/v1/svg/{networkUuid}/{voltageLevelId}", testNetworkId, "notFound"))
                .andExpect(status().isNotFound());

        //network not existing
        mvc.perform(post("/v1/svg/{networkUuid}/{voltageLevelId}", notFoundNetworkId, "vlFr1A"))
                .andExpect(status().isNotFound());

        //variant not existing
        mvc.perform(post("/v1/svg/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "vlFr1A"))
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

        mvc.perform(post("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}", testNetworkId, "vlFr1A"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(post("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_2_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(post("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}?sldDisplayMode=" + SldDisplayMode.FEEDER_POSITION.name() + "&variantId=" + VARIANT_2_ID, testNetworkId, "vlFr1A"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        //voltage level not existing
        mvc.perform(post("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}", testNetworkId, "NotFound"))
                .andExpect(status().isNotFound());

        //network not existing
        mvc.perform(post("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}", notFoundNetworkId, "vlFr1A"))
                .andExpect(status().isNotFound());

        //variant not existing
        mvc.perform(post("/v1/svg-and-metadata/{networkUuid}/{voltageLevelId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "vlFr1A"))
            .andExpect(status().isNotFound());
    }

    @Test
    void testSubstations() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        UUID notFoundNetworkId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(createNetwork());
        given(networkStoreService.getNetwork(notFoundNetworkId, null)).willThrow(new PowsyblException());

        MvcResult result = mvc.perform(post("/v1/substation-svg/{networkUuid}/{substationId}", testNetworkId, "subFr1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
                .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        result = mvc.perform(post("/v1/substation-svg/{networkUuid}/{substationId}?variantId=" + VARIANT_1_ID, testNetworkId, "subFr1"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
            .andReturn();
        assertEquals("<?xml", result.getResponse().getContentAsString().substring(0, 5));

        // substation not existing
        mvc.perform(post("/v1/substation-svg/{networkUuid}/{substationId}", testNetworkId, "notFound"))
                .andExpect(status().isNotFound());

        // network not existing
        mvc.perform(post("/v1/substation-svg/{networkUuid}/{substationId}", notFoundNetworkId, "subFr1"))
                .andExpect(status().isNotFound());

        // variant not existing
        mvc.perform(post("/v1/substation-svg/{networkUuid}/{substationId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "subFr1"))
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

        mvc.perform(post("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?substationLayout=horizontal", testNetworkId, "subFr2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(post("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?substationLayout=horizontal", testNetworkId, "subFr2"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(post("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?variantId=" + VARIANT_2_ID + "&substationLayout=vertical", testNetworkId, "subFr2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        mvc.perform(post("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?topologicalColoring=true&substationLayout=horizontal", testNetworkId, "subFr2"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        // substation not existing
        mvc.perform(post("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}", testNetworkId, "NotFound"))
                .andExpect(status().isNotFound());

        // network not existing
        mvc.perform(post("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}", notFoundNetworkId, "subFr2"))
                .andExpect(status().isNotFound());

        // variant not existing
        mvc.perform(post("/v1/substation-svg-and-metadata/{networkUuid}/{substationId}?variantId=" + VARIANT_NOT_FOUND_ID, testNetworkId, "subFr2"))
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

        NadGenerationContext context = NadGenerationContext.builder()
                .network(network)
                .networkUuid(testNetworkId)
                .variantId(VARIANT_1_ID)
                .build();

        networkAreaDiagramService.assignGeoDataCoordinates(context, List.of(network.getSubstation("subFr2")));
        assertEquals(network.getSubstation("subFr2").getExtension(SubstationPosition.class).getCoordinate(), new com.powsybl.iidm.network.extensions.Coordinate(51.507351, 1.127758));
        assertNull(network.getSubstation("subFr1").getExtension(SubstationPosition.class));

        String faultSubstationGeoDataJson = "[{\"id\":\"subFr1\",\"coordinate\":{\"lat\":48.8588443,\"long\":2.2943506}}]";
        given(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_1_ID, List.of("subFr1"))).willReturn(faultSubstationGeoDataJson);
        PowsyblException exception = assertThrows(PowsyblException.class, () ->
            networkAreaDiagramService.assignGeoDataCoordinates(context, List.of(network.getSubstation("subFr1"))));

        // Assert the exception message
        assertEquals("Failed to parse JSON response", exception.getMessage());
        assertNull(network.getSubstation("subFr1").getExtension(SubstationPosition.class));
    }

    @Test
    void testNetworkAreaDiagramWithGeoData() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        UUID notFoundNetworkId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

        given(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_2_ID, List.of("subFr1"))).willReturn(toString(GEO_DATA_SUBSTATIONS));
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        given(networkStoreService.getNetwork(notFoundNetworkId, PreloadingStrategy.COLLECTION)).willThrow(new PowsyblException());
        NadRequestInfos nadRequestInfos = NadRequestInfos.builder()
                .nadConfigUuid(null)
                .filterUuid(null)
                .voltageLevelIds(Set.of("vlFr1A"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.GEOGRAPHICAL_COORDINATES)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestInfos)))
                .andExpect(request().asyncStarted());
        MvcResult result = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();
        String stringResult = result.getResponse().getContentAsString();
        assertTrue(stringResult.contains("svg"));
        assertTrue(stringResult.contains("metadata"));
        assertTrue(stringResult.contains("additionalMetadata"));
        assertTrue(stringResult.contains("<?xml"));

        NadRequestInfos nadRequestInfosNotFound = NadRequestInfos.builder()
                .nadConfigUuid(null)
                .filterUuid(null)
                .voltageLevelIds(Set.of("ThisVlDoesNotExist"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.GEOGRAPHICAL_COORDINATES)
                .build();

        mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestInfosNotFound)))
                .andExpect(request().asyncStarted());
        mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isNotFound()).andReturn();
    }

    @Test
    void testNetworkAreaDiagramFromFilter() throws Exception {
        UUID testNetworkId = UUID.randomUUID();
        UUID filterUuid = UUID.randomUUID();
        UUID filterUuidNotFound = UUID.randomUUID();

        List<IdentifiableAttributes> filterContent = List.of(new IdentifiableAttributes("vlFr1A", IdentifiableType.VOLTAGE_LEVEL, null));

        given(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_2_ID, List.of("subFr1"))).willReturn(toString(GEO_DATA_SUBSTATIONS));
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        given(filterService.exportFilter(testNetworkId, VARIANT_2_ID, filterUuid)).willReturn(filterContent);
        given(filterService.exportFilter(testNetworkId, VARIANT_2_ID, filterUuidNotFound)).willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

        NadRequestInfos nadRequestWithValidFilter = NadRequestInfos.builder()
                .filterUuid(filterUuid)
                .nadConfigUuid(null)
                .voltageLevelIds(Collections.emptySet())
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestWithValidFilter)))
                .andExpect(request().asyncStarted());
        MvcResult validResult = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();
        String validResponseContent = validResult.getResponse().getContentAsString();
        assertTrue(validResponseContent.contains("svg"));
        assertTrue(validResponseContent.contains("metadata"));
        assertTrue(validResponseContent.contains("additionalMetadata"));
        assertTrue(validResponseContent.contains("<?xml"));

        NadRequestInfos invalidFilterNadRequestJson = NadRequestInfos.builder()
                .filterUuid(filterUuidNotFound)
                .nadConfigUuid(null)
                .voltageLevelIds(Collections.emptySet())
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .build();

        mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidFilterNadRequestJson)))
                .andExpect(request().asyncStarted());
        mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isNotFound()).andReturn();
    }

    @Test
    void testNetworkAreaDiagramWithValidConfig() throws Exception {
        UUID networkUuid = UUID.randomUUID();
        UUID validConfigUuid = UUID.randomUUID();

        NadConfigInfos validConfig = NadConfigInfos.builder()
                .id(validConfigUuid)
                .voltageLevelIds(Set.of("vlFr1A"))
                .scalingFactor(0)
                .positions(List.of())
                .build();

        given(networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION))
                .willReturn(createNetwork());
        given(nadConfigRepository.findWithVoltageLevelIdsById(validConfigUuid))
                .willReturn(Optional.of(validConfig.toEntity()));

        NadRequestInfos requestWithValidConfig = NadRequestInfos.builder()
                .filterUuid(null)
                .nadConfigUuid(validConfigUuid)
                .voltageLevelIds(Collections.emptySet())
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", networkUuid)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithValidConfig)))
                .andExpect(request().asyncStarted());
        MvcResult validResult = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String validResponseContent = validResult.getResponse().getContentAsString();

        assertTrue(validResponseContent.contains("svg"));
        assertTrue(validResponseContent.contains("metadata"));
        assertTrue(validResponseContent.contains("additionalMetadata"));
        assertTrue(validResponseContent.contains("<?xml"));
    }

    @Test
    void testNetworkAreaDiagramWithPositionsFromUser() throws Exception {
        // We want to test if the user's defined positions are not overridden by the nad config's positions.
        UUID networkUuid = UUID.randomUUID();
        UUID validConfigUuid = UUID.randomUUID();

        // First test, with positions from the nad config
        NadVoltageLevelPositionInfos positionFromConfig = NadVoltageLevelPositionInfos.builder()
                .voltageLevelId("vlFr1A")
                .xPosition(75416.26)
                .yPosition(12326.69)
                .xLabelPosition(846.1)
                .yLabelPosition(791.1)
                .build();
        NadConfigInfos nadConfig = NadConfigInfos.builder()
                .id(validConfigUuid)
                .voltageLevelIds(Set.of("vlFr1A"))
                .scalingFactor(0)
                .positions(List.of(positionFromConfig))
                .build();

        given(networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION))
                .willReturn(createNetwork());
        given(nadConfigRepository.findWithVoltageLevelIdsById(validConfigUuid))
                .willReturn(Optional.of(nadConfig.toEntity()));

        NadRequestInfos requestWithPositionFromNadConfig = NadRequestInfos.builder()
                .filterUuid(null)
                .nadConfigUuid(validConfigUuid)
                .voltageLevelIds(Collections.emptySet())
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", networkUuid)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithPositionFromNadConfig)))
                .andExpect(request().asyncStarted());
        MvcResult firstResult = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String firstResultContent = firstResult.getResponse().getContentAsString();

        assertTrue(firstResultContent.contains("75416.26")); // Positions from the nad config
        assertTrue(firstResultContent.contains("12326.69"));
        assertTrue(firstResultContent.contains("846.1"));
        assertTrue(firstResultContent.contains("791.1"));

        // Second test, with positions from the user
        NadVoltageLevelPositionInfos positionFromUser = NadVoltageLevelPositionInfos.builder()
                .voltageLevelId("vlFr1A")
                .xPosition(88588.25)
                .yPosition(99199.85)
                .xLabelPosition(641.2)
                .yLabelPosition(932.2)
                .build();

        NadRequestInfos requestWithPositionsFromUser = NadRequestInfos.builder()
                .filterUuid(null)
                .nadConfigUuid(null)
                .voltageLevelIds(Set.of("vlFr1A"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(List.of(positionFromUser))
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", networkUuid)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithPositionsFromUser)))
                .andExpect(request().asyncStarted());
        MvcResult secondResult = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String secondResultContent = secondResult.getResponse().getContentAsString();

        assertTrue(secondResultContent.contains("88588.25")); // Positions from the user
        assertTrue(secondResultContent.contains("99199.85"));
        assertTrue(secondResultContent.contains("641.2"));
        assertTrue(secondResultContent.contains("932.2"));

        // final test, with positions from both the nad config and the user
        NadRequestInfos requestWithPositionsFromBoth = NadRequestInfos.builder()
                .filterUuid(null)
                .nadConfigUuid(validConfigUuid)
                .voltageLevelIds(Set.of("vlFr1A"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(List.of(positionFromUser))
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", networkUuid)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithPositionsFromBoth)))
                .andExpect(request().asyncStarted());
        MvcResult thirdResult = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();

        String thirdResultContent = thirdResult.getResponse().getContentAsString();

        assertTrue(thirdResultContent.contains("88588.25")); // Positions from the user
        assertTrue(thirdResultContent.contains("99199.85"));
        assertFalse(thirdResultContent.contains("75416.26")); // We do not want the positions from the nad config
        assertFalse(thirdResultContent.contains("12326.69"));
    }

    @Test
    void testNetworkAreaDiagramWithConfigNeverFetchGeodata() throws Exception {
        UUID networkUuid = UUID.randomUUID();
        UUID nadConfigUuid = UUID.randomUUID();

        NadConfigInfos nadConfig = NadConfigInfos.builder()
                .id(nadConfigUuid)
                .voltageLevelIds(Set.of("vlFr1A"))
                .scalingFactor(0)
                .positions(List.of())
                .build();

        given(networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION))
                .willReturn(createNetwork());
        given(nadConfigRepository.findWithVoltageLevelIdsById(nadConfigUuid))
                .willReturn(Optional.of(nadConfig.toEntity()));

        NadRequestInfos requestWithValidConfig = NadRequestInfos.builder()
                .filterUuid(null)
                .nadConfigUuid(nadConfigUuid)
                .voltageLevelIds(Collections.emptySet())
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", networkUuid)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithValidConfig)))
                .andExpect(request().asyncStarted());
        mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk()).andReturn();

        verify(geoDataService, times(0)).getSubstationsGraphics(any(), any(), any());
    }

    @Test
    void testNetworkAreaDiagramWithInvalidConfig() throws Exception {
        UUID networkUuid = UUID.randomUUID();
        UUID configWithInvalidVlUuid = UUID.randomUUID();

        NadConfigInfos configWithInvalidVl = NadConfigInfos.builder()
                .id(configWithInvalidVlUuid)
                .voltageLevelIds(Set.of("notFound"))
                .scalingFactor(0)
                .positions(List.of())
                .build();

        given(networkStoreService.getNetwork(networkUuid, PreloadingStrategy.COLLECTION))
                .willReturn(createNetwork());
        given(nadConfigRepository.findWithVoltageLevelIdsById(configWithInvalidVlUuid))
                .willReturn(Optional.of(configWithInvalidVl.toEntity()));

        NadRequestInfos requestWithValidConfig = NadRequestInfos.builder()
                .filterUuid(null)
                .nadConfigUuid(configWithInvalidVlUuid)
                .voltageLevelIds(Collections.emptySet())
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", networkUuid)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithValidConfig)))
                .andExpect(request().asyncStarted());
        mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isNotFound()).andReturn();
    }

    @ParameterizedTest
    @EnumSource(value = NadPositionsGenerationMode.class, names = {"AUTOMATIC", "GEOGRAPHICAL_COORDINATES", "CONFIGURED"})
    void testNadGeneration(NadPositionsGenerationMode nadPositionsGenerationMode) throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        given(geoDataService.getSubstationsGraphics(testNetworkId, VARIANT_2_ID, List.of("subFr1"))).willReturn(toString(GEO_DATA_SUBSTATIONS));

        NadVoltageLevelPositionInfos vlPositionInfos = NadVoltageLevelPositionInfos.builder()
                .voltageLevelId("vlFr1A")
                .xPosition(1.0)
                .yPosition(1.1)
                .xLabelPosition(1.2)
                .yLabelPosition(1.3)
                .build();

        NadConfigInfos validConfig = NadConfigInfos.builder()
                .id(UUID.randomUUID())
                .voltageLevelIds(Set.of("vlFr1A"))
                .scalingFactor(0)
                .positions(List.of(vlPositionInfos))
                .build();

        given(nadConfigRepository.findById(any())).willReturn(Optional.of(validConfig.toEntity()));
        given(nadVoltageLevelConfiguredPositionRepository.findAll()).willReturn(List.of(vlPositionInfos.toConfiguredPositionEntity()));

        NadRequestInfos nadRequestInfos = NadRequestInfos.builder()
                .filterUuid(null)
                .nadConfigUuid(null)
                .nadPositionsGenerationMode(nadPositionsGenerationMode)
                .voltageLevelIds(Set.of("vlFr1A"))
                .build();

        networkAreaDiagramService.generateNetworkAreaDiagramSvg(testNetworkId, VARIANT_2_ID, nadRequestInfos);
        if (nadPositionsGenerationMode.equals(NadPositionsGenerationMode.GEOGRAPHICAL_COORDINATES)) {
            //initialize with geographical data
            verify(geoDataService, times(1)).getSubstationsGraphics(testNetworkId, VARIANT_2_ID, List.of("subFr1"));
        } else {
            //initialize without geographical data
            verify(geoDataService, times(0)).getSubstationsGraphics(any(), any(), any());
        }
    }

    Network createNetworkWithDepth() {
        Network networkWithDepth = createNetwork();
        // We add lines to create depth
        networkWithDepth.newLine()
            .setId("l1")
            .setVoltageLevel1("vlFr1A")
            .setBus1("busFr1A")
            .setVoltageLevel2("vlFr2A")
            .setBus2("busFr2A")
            .setR(1)
            .setX(3)
            .add();
        networkWithDepth.newLine()
            .setId("l2")
            .setVoltageLevel1("vlFr2A")
            .setBus1("busFr2A")
            .setVoltageLevel2("vlEs1B")
            .setBus2("busEs1B")
            .setR(1)
            .setX(3)
            .add();
        return networkWithDepth;
    }

    @Test
    void testNetworkAreaDiagram() throws Exception {
        UUID testNetworkId = UUID.randomUUID();
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetworkWithDepth());

        NadRequestInfos nadRequestInfos = NadRequestInfos.builder()
                .nadConfigUuid(null)
                .filterUuid(null)
                .voltageLevelIds(Set.of("vlFr1A"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestInfos)))
                .andExpect(request().asyncStarted());
        MvcResult result = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andReturn();
        String stringResult = result.getResponse().getContentAsString();
        assertTrue(stringResult.contains("\"additionalMetadata\":{\"nbVoltageLevels\":1"));
        assertTrue(stringResult.contains("\"voltageLevels\":[{\"id\":\"vlFr1A\",\"name\":\"vlFr1A\",\"substationId\":\"subFr1\""));
    }

    @Test
    void testNetworkAreaDiagramGenerationWithEmptyVoltageLevelPositions() throws Exception {
        UUID testNetworkId = UUID.randomUUID();
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetworkWithDepth());

        NadRequestInfos nadRequestInfos = NadRequestInfos.builder()
                .nadConfigUuid(null)
                .filterUuid(null)
                .voltageLevelIds(Set.of("vlFr1A"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.CONFIGURED)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestInfos)))
                .andExpect(request().asyncStarted());
        mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isBadRequest())
                .andReturn();

    }

    @Test
    void testNetworkAreaDiagramExtension() throws Exception {
        UUID testNetworkId = UUID.randomUUID();
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetworkWithDepth());

        NadRequestInfos nadRequestInfosExtendedVl = NadRequestInfos.builder()
                .nadConfigUuid(null)
                .filterUuid(null)
                .voltageLevelIds(Collections.emptySet())
                .voltageLevelToExpandIds(Set.of("vlFr1A"))
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestInfosExtendedVl)))
                .andExpect(request().asyncStarted());
        MvcResult resultExtendedVl = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andReturn();
        String stringResultExtendedVl = resultExtendedVl.getResponse().getContentAsString();
        assertTrue(stringResultExtendedVl.contains("\"additionalMetadata\":{\"nbVoltageLevels\":2"));
        assertTrue(stringResultExtendedVl.contains("{\"id\":\"vlFr1A\",\"name\":\"vlFr1A\",\"substationId\":\"subFr1\""));
        assertTrue(stringResultExtendedVl.contains("{\"id\":\"vlFr2A\",\"name\":\"vlFr2A\",\"substationId\":\"subFr2\""));
    }

    @Test
    void testNetworkAreaDiagramWithViolationDefaultClass() throws Exception {
        UUID testNetworkId = UUID.randomUUID();
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());

        NadRequestInfos nadRequestInfos = NadRequestInfos.builder()
                .nadConfigUuid(null)
                .filterUuid(null)
                .voltageLevelIds(Set.of("vlFr3A"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .currentLimitViolationsInfos(List.of(
                    CurrentLimitViolationInfos.builder()
                        .equipmentId("twt1")
                        .limitName(null)
                        .build()
                ))
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestInfos)))
                .andExpect(request().asyncStarted());
        MvcResult result = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains(StyleProvider.LINE_OVERLOADED_CLASS));
    }

    @Test
    void testNetworkAreaDiagramWithViolationSanitizedClass() throws Exception {
        UUID testNetworkId = UUID.randomUUID();
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());

        NadRequestInfos nadRequestInfos = NadRequestInfos.builder()
                .nadConfigUuid(null)
                .filterUuid(null)
                .voltageLevelIds(Set.of("vlFr3A"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .currentLimitViolationsInfos(List.of(
                    CurrentLimitViolationInfos.builder()
                        .equipmentId("twt1")
                        .limitName("IT20")
                        .build()
                ))
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestInfos)))
                .andExpect(request().asyncStarted());
        MvcResult result = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("-it20"));
    }

    @Test
    void testSingleLineDiagramWithViolationDefaultClass() {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(createNetworkWithDepth());

        SingleLineDiagramParameters parameters = SingleLineDiagramParameters.builder()
                .useName(true)
                .labelCentered(false)
                .diagonalLabel(true)
                .topologicalColoring(true)
                .componentLibrary(GridSuiteAndConvergenceComponentLibrary.NAME)
                .substationLayout("horizontal")
                .sldDisplayMode(SldDisplayMode.STATE_VARIABLE)
                .language("en")
                .build();

        CurrentLimitViolationInfos violation = CurrentLimitViolationInfos.builder()
                .equipmentId("twt1")
                .limitName(null)
                .build();

        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(testNetworkId, VARIANT_2_ID, "subFr3", parameters, List.of(violation));
        String svg = svgAndMetadata.getSvg();
        assertNotNull(svg);
        assertTrue(svg.contains(OVERLOAD_STYLE_CLASS));
    }

    @Test
    void testSingleLineDiagramNodeBreakerWithExtensions() {
        UUID testNetworkId = UUID.fromString(UUID.randomUUID().toString());
        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(FourSubstationsNodeBreakerWithExtensionsFactory.create());

        SingleLineDiagramParameters parameters = SingleLineDiagramParameters.builder()
            .useName(true)
            .labelCentered(false)
            .diagonalLabel(true)
            .topologicalColoring(true)
            .componentLibrary(GridSuiteAndConvergenceComponentLibrary.NAME)
            .substationLayout("horizontal")
            .sldDisplayMode(SldDisplayMode.STATE_VARIABLE)
            .language("en")
            .build();

        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(testNetworkId, null, "S1VL1", parameters, List.of());
        String svg = svgAndMetadata.getSvg();
        assertNotNull(svg);
    }

    @Test
    void testSingleLineDiagramNodeBreakerWithoutExtensions() {
        UUID testNetworkId = UUID.fromString(UUID.randomUUID().toString());
        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(FourSubstationsNodeBreakerFactory.create());

        SingleLineDiagramParameters parameters = SingleLineDiagramParameters.builder()
            .useName(true)
            .labelCentered(false)
            .diagonalLabel(true)
            .topologicalColoring(true)
            .componentLibrary(GridSuiteAndConvergenceComponentLibrary.NAME)
            .substationLayout("horizontal")
            .sldDisplayMode(SldDisplayMode.STATE_VARIABLE)
            .language("en")
            .build();

        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(testNetworkId, null, "S1VL1", parameters, List.of());
        String svg = svgAndMetadata.getSvg();
        assertNotNull(svg);
    }

    @Test
    void testSingleLineDiagramWithViolationSanitizedClass() {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(createNetworkWithDepth());

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

        CurrentLimitViolationInfos violation = CurrentLimitViolationInfos.builder()
                .equipmentId("twt1")
                .limitName("IT20")
                .build();

        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(testNetworkId, VARIANT_2_ID, "subFr3", parameters, List.of(violation));
        String svg = svgAndMetadata.getSvg();
        assertNotNull(svg);
        String expected = OVERLOAD_STYLE_CLASS + "-it20";
        assertTrue(svg.contains(expected));
    }

    @Test
    void testCreatePositionsFromCsv() throws Exception {

        byte[] voltageLevelBytes = IOUtils.toByteArray(new FileInputStream(ResourceUtils.getFile("classpath:voltage-level-positions.csv")));
        MockMultipartFile file = new MockMultipartFile("file", "vl-positions.csv", "text/csv", voltageLevelBytes);
        mvc.perform(MockMvcRequestBuilders.multipart("/v1/network-area-diagram/config/positions")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    void testCreatePositionsFromInvalidCsvContentType() throws Exception {
        byte[] voltageLevelBytes = IOUtils.toByteArray(new FileInputStream(ResourceUtils.getFile("classpath:voltage-level-positions.csv")));
        MockMultipartFile file = new MockMultipartFile("file", "vl-positions.csv", "invalidContentType", voltageLevelBytes);
        mvc.perform(MockMvcRequestBuilders.multipart("/v1/network-area-diagram/config/positions")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Invalid CSV format!"));
    }

    @Test
    void testCreatePositionsFromInvalidCsvHeader() throws Exception {
        byte[] voltageLevelBytes = IOUtils.toByteArray(new FileInputStream(ResourceUtils.getFile("classpath:voltage-level-positions-invalid-header.csv")));
        MockMultipartFile file = new MockMultipartFile("file", "vl-positions.csv", "text/csv", voltageLevelBytes);
        mvc.perform(MockMvcRequestBuilders.multipart("/v1/network-area-diagram/config/positions")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("The csv headers are invalid!"));
    }

    @Test
    void testCreatePositionsFromEmptyCsv() throws Exception {
        byte[] voltageLevelBytes = IOUtils.toByteArray(new FileInputStream(ResourceUtils.getFile("classpath:voltage-level-positions-empty.csv")));
        MockMultipartFile file = new MockMultipartFile("file", "vl-positions.csv", "text/csv", voltageLevelBytes);
        mvc.perform(MockMvcRequestBuilders.multipart("/v1/network-area-diagram/config/positions")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("No positions found!"));
    }

    @Test
    void testCreatingPositionsFromCsvMultipleTimes() throws Exception {

        byte[] voltageLevelBytes = IOUtils.toByteArray(new FileInputStream(ResourceUtils.getFile("classpath:voltage-level-positions.csv")));
        MockMultipartFile file = new MockMultipartFile("file", "vl-positions.csv", "text/csv", voltageLevelBytes);
        int expectedRowCount = 5;
        mvc.perform(MockMvcRequestBuilders.multipart("/v1/network-area-diagram/config/positions")
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
        mvc.perform(MockMvcRequestBuilders.multipart("/v1/network-area-diagram/config/positions")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk());
        actualRowCount = nadVoltageLevelConfiguredPositionRepository.count();
        assertEquals(expectedRowCount, actualRowCount);
    }

    @Test
    void testNetworkAreaDiagramOmition() throws Exception {
        UUID testNetworkId = UUID.randomUUID();
        UUID filterUuid = UUID.randomUUID();

        List<IdentifiableAttributes> filterContent = List.of(
                new IdentifiableAttributes("vlFr1A", IdentifiableType.VOLTAGE_LEVEL, null),
                new IdentifiableAttributes("vlFr2A", IdentifiableType.VOLTAGE_LEVEL, null)
        );

        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());
        given(filterService.exportFilter(testNetworkId, VARIANT_2_ID, filterUuid)).willReturn(filterContent);

        // First we test that the NAD contains 3 voltage levels (two come from the filter used)
        NadRequestInfos nadRequestNoOmition = NadRequestInfos.builder()
                .filterUuid(filterUuid)
                .nadConfigUuid(null)
                .voltageLevelIds(Set.of("vlEs1B"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestNoOmition)))
                .andExpect(request().asyncStarted());
        MvcResult resultNoOmition = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();
        String stringResultNoOmition = resultNoOmition.getResponse().getContentAsString();
        assertTrue(stringResultNoOmition.contains("\"additionalMetadata\":{\"nbVoltageLevels\":3"));
        assertTrue(stringResultNoOmition.contains("{\"id\":\"vlFr1A\",\"name\":\"vlFr1A\",\"substationId\":\"subFr1\""));
        assertTrue(stringResultNoOmition.contains("{\"id\":\"vlFr2A\",\"name\":\"vlFr2A\",\"substationId\":\"subFr2\""));
        assertTrue(stringResultNoOmition.contains("{\"id\":\"vlEs1B\",\"name\":\"vlEs1B\",\"substationId\":\"subEs1\""));

        // Then we test that the omition system removes the voltage levels (even if they were initially in the filter)
        NadRequestInfos nadRequestWithOmition = NadRequestInfos.builder()
                .filterUuid(filterUuid)
                .nadConfigUuid(null)
                .voltageLevelIds(Set.of("vlEs1B"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Set.of("vlFr2A"))
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestWithOmition)))
                .andExpect(request().asyncStarted());
        MvcResult resultWithOmition = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();
        String stringResultWithOmition = resultWithOmition.getResponse().getContentAsString();
        assertTrue(stringResultWithOmition.contains("\"additionalMetadata\":{\"nbVoltageLevels\":2"));
        assertTrue(stringResultWithOmition.contains("{\"id\":\"vlFr1A\",\"name\":\"vlFr1A\",\"substationId\":\"subFr1\""));
        assertFalse(stringResultWithOmition.contains("{\"id\":\"vlFr2A\",\"name\":\"vlFr2A\",\"substationId\":\"subFr2\""));
        assertTrue(stringResultWithOmition.contains("{\"id\":\"vlEs1B\",\"name\":\"vlEs1B\",\"substationId\":\"subEs1\""));
    }

    @Test
    void testNetworkAreaDiagramOmitedAndExtension() throws Exception {
        UUID testNetworkId = UUID.randomUUID();
        UUID filterUuid = UUID.randomUUID();

        List<IdentifiableAttributes> filterContent = List.of(
                new IdentifiableAttributes("vlFr1A", IdentifiableType.VOLTAGE_LEVEL, null),
                new IdentifiableAttributes("vlFr2A", IdentifiableType.VOLTAGE_LEVEL, null)
        );

        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetworkWithDepth());
        given(filterService.exportFilter(testNetworkId, null, filterUuid)).willReturn(filterContent);

        // If a VL was omited but the user requested to expand another VL that would add again the omited VL,
        // then the omited VL would be added to the SVG.
        NadRequestInfos nadRequestWithOmitionAndExtension = NadRequestInfos.builder()
                .filterUuid(filterUuid) // Adds vlFr1A, vlFr2A
                .nadConfigUuid(null)
                .voltageLevelIds(Set.of("vlEs1B")) // Adds vlEs1B
                .voltageLevelToExpandIds(Set.of("vlFr1A")) // Adds vlFr2A (vlFr1A's neighour)
                .voltageLevelToOmitIds(Set.of("vlFr2A"))
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestWithOmitionAndExtension)))
                .andExpect(request().asyncStarted());
        MvcResult resultWithOmitionAndExtension = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON_VALUE))
                .andReturn();
        String stringResultWithOmitionAndExtension = resultWithOmitionAndExtension.getResponse().getContentAsString();
        assertTrue(stringResultWithOmitionAndExtension.contains("\"additionalMetadata\":{\"nbVoltageLevels\":3"));
        assertTrue(stringResultWithOmitionAndExtension.contains("{\"id\":\"vlFr1A\",\"name\":\"vlFr1A\",\"substationId\":\"subFr1\""));
        assertTrue(stringResultWithOmitionAndExtension.contains("{\"id\":\"vlFr2A\",\"name\":\"vlFr2A\",\"substationId\":\"subFr2\""));
        assertTrue(stringResultWithOmitionAndExtension.contains("{\"id\":\"vlEs1B\",\"name\":\"vlEs1B\",\"substationId\":\"subEs1\""));
    }

    @Test
    void testNetworkAreaDiagramTooManyVoltageLevels() throws Exception {
        int maxVls = 2;
        ReflectionTestUtils.setField(networkAreaDiagramService, "maxVoltageLevels", maxVls);

        UUID testNetworkId = UUID.randomUUID();
        given(networkStoreService.getNetwork(testNetworkId, PreloadingStrategy.COLLECTION)).willReturn(createNetwork());

        Set vlIds = Set.of("vlFr1A", "vlFr1B", "vlFr2A");
        NadRequestInfos nadRequestInfos = NadRequestInfos.builder()
            .nadConfigUuid(null)
            .filterUuid(null)
            .voltageLevelIds(vlIds)
            .voltageLevelToExpandIds(Collections.emptySet())
            .voltageLevelToOmitIds(Collections.emptySet())
            .positions(Collections.emptyList())
            .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
            .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nadRequestInfos)))
                .andExpect(request().asyncStarted());
        MvcResult mvcResult = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
            .andExpect(status().isForbidden())
            .andReturn();
        String errorMessage = mvcResult.getResponse().getErrorMessage();
        assertEquals(String.format("You need to reduce the number of voltage levels to be displayed in the network area diagram (current %s, maximum %s)", vlIds.size(), maxVls), errorMessage);
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

        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(testNetworkId, VARIANT_2_ID, "vlFr1A", parameters, null);
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

        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(testNetworkId, VARIANT_2_ID, "subFr1", parameters, null);
        Object additionalMetadata = svgAndMetadata.getAdditionalMetadata();
        assertNotNull(additionalMetadata);
        Map<String, String> convertedMetadata = objectMapper.convertValue(additionalMetadata, new TypeReference<>() { });
        assertEquals("subFr1", convertedMetadata.get("id"));
        assertNull(convertedMetadata.get("name"));
        assertEquals("FR", convertedMetadata.get("country"));
    }

    /**
     * @return a network with 2 voltage level - vl1 with 2 calculated buses, vl2 with 1 calculated bus
     */
    public static Network createTwoVoltageLevels() {
        Network network = Network.create("dl", "test");
        Substation s = network.newSubstation().setId("s1").setName("Substation 1").add();
        VoltageLevel vl1 = s.newVoltageLevel()
            .setId("vl1")
            .setName("Voltage level 1")
            .setNominalV(400)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .add();
        vl1.getBusBreakerView().newBus()
            .setId("busId11")
            .add();
        vl1.newGenerator()
            .setId("g11")
            .setConnectableBus("busId11")
            .setBus("busId11")
            .setTargetP(101.3664)
            .setTargetV(390)
            .setMinP(0)
            .setMaxP(150)
            .setVoltageRegulatorOn(true)
            .add();
        vl1.getBusBreakerView().newBus()
            .setId("busId12")
            .add();
        vl1.newGenerator()
            .setId("g12")
            .setConnectableBus("busId12")
            .setBus("busId12")
            .setTargetP(101.3664)
            .setTargetV(390)
            .setMinP(0)
            .setMaxP(150)
            .setVoltageRegulatorOn(true)
            .add();
        VoltageLevel vl2 = s.newVoltageLevel()
            .setId("vl2")
            .setName("Voltage level 2")
            .setNominalV(400)
            .setTopologyKind(TopologyKind.BUS_BREAKER)
            .add();
        vl2.getBusBreakerView().newBus()
            .setId("busId2")
            .add();
        vl2.newDanglingLine()
            .setId("dl1")
            .setConnectableBus("busId2")
            .setBus("busId2")
            .setR(0.7)
            .setX(1)
            .setG(1e-6)
            .setB(3e-6)
            .setP0(101)
            .setQ0(150)
            .newGeneration()
            .setTargetP(0)
            .setTargetQ(0)
            .setTargetV(390)
            .setVoltageRegulationOn(false)
            .add()
            .add();
        network.newLine()
            .setId("l1")
            .setVoltageLevel1("vl1")
            .setBus1("busId11")
            .setVoltageLevel2("vl2")
            .setBus2("busId2")
            .setR(1)
            .setX(3)
            .setG1(0)
            .setG2(0)
            .setB1(0)
            .setB2(0)
            .add();
        return network;
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

        Substation substationFr3 = network.newSubstation()
                .setId("subFr3")
                .setCountry(Country.FR)
                .setTso("RTE")
                .add();
        VoltageLevel voltageLevelFr3A = substationFr3.newVoltageLevel()
                .setId("vlFr3A")
                .setName("vlFr3A")
                .setNominalV(440.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelFr3A.getBusBreakerView().newBus()
                .setId("busFr3A")
                .setName("busFr3A")
                .add();

        VoltageLevel voltageLevelFr3B = substationFr3.newVoltageLevel()
                .setId("vlFr3B")
                .setName("vlFr3B")
                .setNominalV(440.0)
                .setHighVoltageLimit(400.0)
                .setLowVoltageLimit(200.0)
                .setTopologyKind(TopologyKind.BUS_BREAKER)
                .add();
        voltageLevelFr3B.getBusBreakerView().newBus()
                .setId("busFr3B")
                .setName("busFr3B")
                .add();

        substationFr3.newTwoWindingsTransformer()
                .setId("twt1")
                .setName("twt1")
                .setVoltageLevel1("vlFr3A")
                .setBus1("busFr3A")
                .setVoltageLevel2("vlFr3B")
                .setBus2("busFr3B")
                .setConnectableBus1("busFr3A")
                .setConnectableBus2("busFr3B")
                .setRatedU2(158.)
                .setR(47)
                .setG(27)
                .setB(17)
                .setX(23)
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
        sldParameters.setStyleProviderFactory((n, p) -> new NominalVoltageStyleProvider());
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

        NadRequestInfos nadRequestInfos = NadRequestInfos.builder()
                .nadConfigUuid(null)
                .filterUuid(null)
                .voltageLevelIds(Set.of("vlFr1A", "vlNotFound1"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .nadPositionsGenerationMode(NadPositionsGenerationMode.AUTOMATIC)
                .build();

        ResultActions mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestInfos)))
                .andExpect(request().asyncStarted());
        MvcResult result = mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isOk())
                .andReturn();
        String stringResult = result.getResponse().getContentAsString();
        assertEquals("{\"svg\":\"<?xml", stringResult.substring(0, 13));
        assertTrue(stringResult.contains("\"equipmentId\" : \"vlFr1A\""));
        assertTrue(stringResult.contains("\"additionalMetadata\":{\"nbVoltageLevels\":1"));

        NadRequestInfos nadRequestInfosVlNotFound = NadRequestInfos.builder()
                .nadConfigUuid(null)
                .filterUuid(null)
                .voltageLevelIds(Set.of("vlNotFound1", "vlNotFound2"))
                .voltageLevelToExpandIds(Collections.emptySet())
                .voltageLevelToOmitIds(Collections.emptySet())
                .positions(Collections.emptyList())
                .build();

        mockMvcResultActions = mvc.perform(post("/v1/network-area-diagram/{networkUuid}", testNetworkId)
                        .param("variantId", VARIANT_2_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nadRequestInfosVlNotFound)))
                .andExpect(request().asyncStarted());
        mvc.perform(asyncDispatch(mockMvcResultActions.andReturn()))
                .andExpect(status().isNotFound()).andReturn();
    }

    @Test
    void testBusLegendContainsBusId() throws Exception {
        UUID testNetworkId = UUID.fromString("7928181c-7977-4592-ba19-88027e4254e4");
        given(networkStoreService.getNetwork(testNetworkId, null)).willReturn(createTwoVoltageLevels());

        MvcResult result = mvc.perform(post("/v1/svg/{networkUuid}/{voltageLevelId}", testNetworkId, "vl1"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
            .andReturn();
        String stringResult = result.getResponse().getContentAsString();
        assertEquals("<?xml", stringResult.substring(0, 5));
        // vl1 should have 2 busId displayed in bus legend
        assertTrue(stringResult.contains(">vl1_0<"));
        assertTrue(stringResult.contains(">vl1_1<"));

        result = mvc.perform(post("/v1/svg/{networkUuid}/{voltageLevelId}", testNetworkId, "vl2"))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(SingleLineDiagramController.IMAGE_SVG_PLUS_XML))
            .andReturn();
        stringResult = result.getResponse().getContentAsString();
        assertEquals("<?xml", stringResult.substring(0, 5));
        // vl1 should have 1 busId displayed in bus legend
        assertTrue(stringResult.contains(">vl2_0<"));
    }

    private static String toString(String resourceName) throws IOException {
        return new String(ByteStreams.toByteArray(Objects.requireNonNull(SingleLineDiagramTest.class.getResourceAsStream(resourceName))), StandardCharsets.UTF_8);
    }

    @Test
    void testCreateMultipleNetworkAreaDiagramConfigs() throws Exception {
        NadConfigInfos config1 = NadConfigInfos.builder()
                .voltageLevelIds(Set.of("vlFr1A"))
                .scalingFactor(100000)
                .positions(Collections.emptyList())
                .build();

        NadConfigInfos config2 = NadConfigInfos.builder()
                .voltageLevelIds(Set.of("vlFr2A"))
                .scalingFactor(200000)
                .positions(Collections.emptyList())
                .build();

        List<NadConfigInfos> configs = List.of(config1, config2);

        mvc.perform(post("/v1/network-area-diagram/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(configs)))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateMultipleNetworkAreaDiagramConfigsWithEmptyList() throws Exception {
        List<NadConfigInfos> emptyConfigs = Collections.emptyList();

        mvc.perform(post("/v1/network-area-diagram/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyConfigs)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteMultipleNetworkAreaDiagramConfigs() throws Exception {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        List<UUID> configUuids = List.of(uuid1, uuid2);

        mvc.perform(delete("/v1/network-area-diagram/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(configUuids)))
                .andExpect(status().isOk());
    }

    @Test
    void testDeleteMultipleNetworkAreaDiagramConfigsWithEmptyList() throws Exception {
        List<UUID> emptyUuids = Collections.emptyList();

        mvc.perform(delete("/v1/network-area-diagram/configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyUuids)))
                .andExpect(status().isOk());
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
