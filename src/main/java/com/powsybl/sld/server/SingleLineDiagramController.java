/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.RawValue;
import com.powsybl.commons.config.BaseVoltagesConfig;
import com.powsybl.sld.server.dto.SvgAndMetadata;
import com.powsybl.sld.server.dto.SvgGenerationMetadata;
import com.powsybl.sld.server.dto.nad.NadConfigInfos;
import com.powsybl.sld.server.dto.nad.NadRequestInfos;
import com.powsybl.sld.server.dto.sld.SldRequestInfos;
import com.powsybl.sld.server.utils.SingleLineDiagramParameters;
import com.powsybl.sld.server.utils.SldDisplayMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.powsybl.sld.server.NetworkAreaDiagramService.*;
import static com.powsybl.ws.commons.LogUtils.sanitizeParam;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + SingleLineDiagramApi.API_VERSION + "/")
@Tag(name = "single-line-diagram-server")
@ComponentScan(basePackageClasses = SingleLineDiagramService.class)
public class SingleLineDiagramController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleLineDiagramController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static final String IMAGE_SVG_PLUS_XML = "image/svg+xml";
    static final String HORIZONTAL = "horizontal";

    private final SingleLineDiagramService singleLineDiagramService;

    private final NetworkAreaDiagramService networkAreaDiagramService;

    @Autowired
    private ObjectMapper objectMapper;

    public SingleLineDiagramController(SingleLineDiagramService singleLineDiagramService,
                                       NetworkAreaDiagramService networkAreaDiagramService) {
        this.singleLineDiagramService = singleLineDiagramService;
        this.networkAreaDiagramService = networkAreaDiagramService;
    }

    // voltage levels
    //
    @PostMapping(value = "/svg/{networkUuid}/{voltageLevelId}", produces = IMAGE_SVG_PLUS_XML)
    @Operation(summary = "Get voltage level image")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The voltage level SVG")})
    public @ResponseBody String generateVoltageLevelSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String language,
            @RequestBody(required = false) SvgGenerationMetadata svgGenerationMetadata) {
        LOGGER.debug("getVoltageLevelSvg request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId != null ? sanitizeParam(voltageLevelId) : null);
        var parameters = SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(HORIZONTAL)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, voltageLevelId, parameters, svgGenerationMetadata).getSvg();
    }

    @GetMapping(value = "/metadata/{networkUuid}/{voltageLevelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the voltage level svg metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The voltage level SVG metadata")})
    public String getVoltageLevelMetadata(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String language,
            @RequestBody(required = false) BaseVoltagesConfig baseVoltagesConfig) {
        LOGGER.debug("getVoltageLevelMetadata request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId != null ? sanitizeParam(voltageLevelId) : null);

        var parameters = SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(HORIZONTAL)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        SldRequestInfos sldRequestInfos = SldRequestInfos.builder()
                .baseVoltagesConfig(baseVoltagesConfig)
                .currentLimitViolationInfos(null)
                .build();
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, voltageLevelId, parameters, sldRequestInfos).getMetadata();
    }

    @PostMapping(value = "svg-and-metadata/{networkUuid}/{voltageLevelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get voltage level svg and metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The voltage level svg and metadata")})
    public @ResponseBody String generateVoltageLevelFullSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String language,
            @RequestBody(required = false) SvgGenerationMetadata svgGenerationMetadata) throws JsonProcessingException {
        LOGGER.debug("getVoltageLevelCompleteSvg request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId != null ? sanitizeParam(voltageLevelId) : null);
        var parameters = SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(HORIZONTAL)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, voltageLevelId, parameters, svgGenerationMetadata);
        String svg = svgAndMetadata.getSvg();
        String metadata = svgAndMetadata.getMetadata();
        Object additionalMetadata = svgAndMetadata.getAdditionalMetadata();
        return OBJECT_MAPPER.writeValueAsString(
                OBJECT_MAPPER.createObjectNode()
                        .put(SVG_TAG, svg)
                        .putRawValue(METADATA, new RawValue(metadata))
                        .putPOJO(ADDITIONAL_METADATA, additionalMetadata));
    }

    // substations
    //
    @PostMapping(value = "/substation-svg/{networkUuid}/{substationId}", produces = IMAGE_SVG_PLUS_XML)
    @Operation(summary = "Get substation image")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The substation svg")})
    public @ResponseBody String generateSubstationSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "Substation ID") @PathVariable("substationId") String substationId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "substationLayout") @RequestParam(name = "substationLayout", defaultValue = HORIZONTAL) String substationLayout,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String language,
            @RequestBody(required = false) SvgGenerationMetadata svgGenerationMetadata) {
        LOGGER.debug("getSubstationSvg request received with parameter networkUuid = {}, substationID = {}", networkUuid, substationId != null ? sanitizeParam(substationId) : null);
        var parameters = SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(substationLayout)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, substationId, parameters, svgGenerationMetadata).getSvg();
    }

    @GetMapping(value = "/substation-metadata/{networkUuid}/{substationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get substation svg metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The substation svg metadata")})
    public String getSubstationMetadata(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "Substation ID") @PathVariable("substationId") String substationId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "substationLayout") @RequestParam(name = "substationLayout", defaultValue = HORIZONTAL) String substationLayout,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String language,
            @RequestBody(required = false) BaseVoltagesConfig baseVoltagesConfig) {
        LOGGER.debug("getSubstationMetadata request received with parameter networkUuid = {}, substationID = {}", networkUuid, substationId != null ? sanitizeParam(substationId) : null);
        var parameters = SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(substationLayout)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        SldRequestInfos sldRequestInfos = SldRequestInfos.builder()
                .baseVoltagesConfig(baseVoltagesConfig)
                .currentLimitViolationInfos(null)
                .build();
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, substationId, parameters, sldRequestInfos).getMetadata();
    }

    @PostMapping(value = "substation-svg-and-metadata/{networkUuid}/{substationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get substation svg and metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The substation svg and metadata")})
    public @ResponseBody String generateSubstationFullSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "Substation ID") @PathVariable("substationId") String substationId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "substationLayout") @RequestParam(name = "substationLayout", defaultValue = HORIZONTAL) String substationLayout,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String language,
            @RequestBody(required = false) SvgGenerationMetadata svgGenerationMetadata) throws JsonProcessingException {
        LOGGER.debug("getSubstationFullSvg request received with parameter networkUuid = {}, substationID = {}", networkUuid, substationId != null ? sanitizeParam(substationId) : null);
        var parameters = SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(substationLayout)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, substationId, parameters, svgGenerationMetadata);
        String svg = svgAndMetadata.getSvg();
        String metadata = svgAndMetadata.getMetadata();
        Object additionalMetadata = svgAndMetadata.getAdditionalMetadata();
        return OBJECT_MAPPER.writeValueAsString(
                OBJECT_MAPPER.createObjectNode()
                        .put(SVG_TAG, svg)
                        .putRawValue(METADATA, new RawValue(metadata))
                        .putPOJO(ADDITIONAL_METADATA, additionalMetadata));
    }

    @GetMapping(value = "/svg-component-libraries")
    @Operation(summary = "Get a list of the available svg component libraries")
    @ApiResponse(responseCode = "200", description = "The list of available svg component libraries")
    public ResponseEntity<Collection<String>> getAvailableSvgComponentLibraries() {
        LOGGER.debug("getAvailableSvgComponentLibraries ...");
        Collection<String> libraries = singleLineDiagramService.getAvailableSvgComponentLibraries();
        return ResponseEntity.ok().body(libraries);
    }

    // network area diagram
    @PostMapping(value = "/network-area-diagram/{networkUuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get network area diagram image")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The network area diagram svg")})
    public CompletableFuture<String> generateNetworkAreaDiagramSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @RequestBody NadRequestInfos nadRequestInfos) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("generateNetworkAreaDiagramSvg request received with parameter networkUuid = {}, body = {}", networkUuid, sanitizeParam(nadRequestInfos.toString()));
        }
        return networkAreaDiagramService.generateNetworkAreaDiagramSvgAsync(networkUuid, variantId, nadRequestInfos);
    }

    @PostMapping(value = "/network-area-diagram/config", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a network area diagram config")
    @ApiResponse(responseCode = "200", description = "The network area diagram config has been created")
    public ResponseEntity<UUID> createNetworkAreaDiagramConfig(@RequestBody NadConfigInfos nadConfigInfos) {
        return ResponseEntity.ok().body(networkAreaDiagramService.createNetworkAreaDiagramConfig(nadConfigInfos));
    }

    @PostMapping("/network-area-diagram/configs")
    @Operation(summary = "Create multiple network area diagram configs")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The created configs UUIDs")})
    public ResponseEntity<List<UUID>> createMultipleNetworkAreaDiagramConfigs(@RequestBody List<NadConfigInfos> nadConfigs) {
        return ResponseEntity.ok().body(networkAreaDiagramService.createNetworkAreaDiagramConfigs(nadConfigs));
    }

    @DeleteMapping("/network-area-diagram/configs")
    @Operation(summary = "Delete multiple network area diagram configs")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The network area diagram configs were successfully deleted")})
    public ResponseEntity<Void> deleteMultipleNetworkAreaDiagramConfigs(@RequestBody List<UUID> configUuids) {
        networkAreaDiagramService.deleteNetworkAreaDiagramConfigs(configUuids);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/network-area-diagram/config", params = "duplicateFrom", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Duplicate a network area diagram config")
    @ApiResponse(responseCode = "200", description = "The network area diagram config has been duplicated")
    public ResponseEntity<UUID> duplicateNetworkAreaDiagramConfig(@RequestParam(name = "duplicateFrom") UUID duplicateFrom) {
        return ResponseEntity.ok().body(networkAreaDiagramService.duplicateNetworkAreaDiagramConfig(duplicateFrom));
    }

    @PutMapping(value = "/network-area-diagram/config/{nadConfigUuid}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a network area diagram config")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "The network area diagram config was updated"),
        @ApiResponse(responseCode = "404", description = "The network area diagram config was not found"),
    })
    public ResponseEntity<Void> updateNetworkAreaDiagramConfig(
            @Parameter(description = "Network Area Diagram config UUID") @PathVariable("nadConfigUuid") UUID nadConfigUuid,
            @RequestBody NadConfigInfos nadConfigInfos) {
        networkAreaDiagramService.updateNetworkAreaDiagramConfig(nadConfigUuid, nadConfigInfos);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/network-area-diagram/config/{nadConfigUuid}")
    @Operation(summary = "Delete a network area diagram config")
    @ApiResponse(responseCode = "200", description = "The network area diagram config has been deleted")
    public ResponseEntity<Void> deleteNetworkAreaDiagramConfig(@Parameter(description = "Network Area Diagram config UUID") @PathVariable("nadConfigUuid") UUID nadConfigUuid) {
        networkAreaDiagramService.deleteNetworkAreaDiagramConfig(nadConfigUuid);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/network-area-diagram/config/positions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get positions coordinates from given CSV file and store them on the DB")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The list of positions has been successfully stored")
    })
    public ResponseEntity<Void> createNadPositionsConfigFromCsv(@RequestParam("file") MultipartFile file) {
        networkAreaDiagramService.createNadPositionsConfigFromCsv(file);
        return ResponseEntity.ok().build();
    }
}
