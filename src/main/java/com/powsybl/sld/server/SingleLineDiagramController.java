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
import com.powsybl.sld.server.utils.SingleLineDiagramParameters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

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

    @Autowired
    private SingleLineDiagramService singleLineDiagramService;

    @Autowired
    private NetworkAreaDiagramService networkAeraDiagramService;

    // voltage levels
    //
    @GetMapping(value = "/svg/{networkUuid}/{voltageLevelId}", produces = IMAGE_SVG_PLUS_XML)
    @Operation(summary = "Get voltage level image")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The voltage level SVG")})
    public @ResponseBody String getVoltageLevelSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "useName") @RequestParam(name = "useFeederPositions", defaultValue = "false") boolean useFeederPositions) {
        LOGGER.debug("getVoltageLevelSvg request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);
        var parameters = new SingleLineDiagramParameters(useName, centerLabel, diagonalLabel, topologicalColoring, componentLibrary, HORIZONTAL, useFeederPositions);
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, voltageLevelId, parameters).getLeft();
    }

    @GetMapping(value = "/metadata/{networkUuid}/{voltageLevelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the voltage level svg metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The voltage level SVG metadata")})
    public @ResponseBody String getVoltageLevelMetadata(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "useName") @RequestParam(name = "useFeederPositions", defaultValue = "false") boolean useFeederPositions) {
        LOGGER.debug("getVoltageLevelMetadata request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);

        var parameters = new SingleLineDiagramParameters(useName, centerLabel, diagonalLabel, topologicalColoring, componentLibrary, HORIZONTAL, useFeederPositions);
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, voltageLevelId, parameters).getRight();
    }

    @GetMapping(value = "svg-and-metadata/{networkUuid}/{voltageLevelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get voltage level svg and metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The voltage level svg and metadata")})
    public @ResponseBody String getVoltageLevelFullSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "useName") @RequestParam(name = "useFeederPositions", defaultValue = "false") boolean useFeederPositions) throws JsonProcessingException {
        LOGGER.debug("getVoltageLevelCompleteSvg request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);

        var parameters = new SingleLineDiagramParameters(useName, centerLabel, diagonalLabel, topologicalColoring, componentLibrary, HORIZONTAL, useFeederPositions);
        Pair<String, String> svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, voltageLevelId, parameters);
        String svg = svgAndMetadata.getLeft();
        String metadata = svgAndMetadata.getRight();
        return OBJECT_MAPPER.writeValueAsString(
                OBJECT_MAPPER.createObjectNode()
                    .put("svg", svg)
                    .putRawValue("metadata", new RawValue(metadata)));
    }

    // substations
    //
    @GetMapping(value = "/substation-svg/{networkUuid}/{substationId}", produces = IMAGE_SVG_PLUS_XML)
    @Operation(summary = "Get substation image")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The substation svg")})
    public @ResponseBody String getSubstationSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "Substation ID") @PathVariable("substationId") String substationId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "substationLayout") @RequestParam(name = "substationLayout", defaultValue = HORIZONTAL) String substationLayout,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "useName") @RequestParam(name = "useFeederPositions", defaultValue = "false") boolean useFeederPositions) {
        LOGGER.debug("getSubstationSvg request received with parameter networkUuid = {}, substationID = {}", networkUuid, substationId);

        var parameters = new SingleLineDiagramParameters(useName, centerLabel, diagonalLabel, topologicalColoring, componentLibrary, substationLayout, useFeederPositions);
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, substationId, parameters).getLeft();
    }

    @GetMapping(value = "/substation-metadata/{networkUuid}/{substationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get substation svg metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The substation svg metadata")})
    public @ResponseBody String getSubstationMetadata(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "Substation ID") @PathVariable("substationId") String substationId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "substationLayout") @RequestParam(name = "substationLayout", defaultValue = HORIZONTAL) String substationLayout,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "useName") @RequestParam(name = "useFeederPositions", defaultValue = "false") boolean useFeederPositions) {
        LOGGER.debug("getSubstationMetadata request received with parameter networkUuid = {}, substationID = {}", networkUuid, substationId);

        var parameters = new SingleLineDiagramParameters(useName, centerLabel, diagonalLabel, topologicalColoring, componentLibrary, substationLayout, useFeederPositions);
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, substationId, parameters).getRight();
    }

    @GetMapping(value = "substation-svg-and-metadata/{networkUuid}/{substationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get substation svg and metadata")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The substation svg and metadata")})
    public @ResponseBody String getSubstationFullSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "Substation ID") @PathVariable("substationId") String substationId,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @Parameter(description = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @Parameter(description = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @Parameter(description = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring,
            @Parameter(description = "substationLayout") @RequestParam(name = "substationLayout", defaultValue = HORIZONTAL) String substationLayout,
            @Parameter(description = "component library name") @RequestParam(name = "componentLibrary", defaultValue = GridSuiteAndConvergenceComponentLibrary.NAME) String componentLibrary,
            @Parameter(description = "useName") @RequestParam(name = "useFeederPositions", defaultValue = "false") boolean useFeederPositions) throws JsonProcessingException {
        LOGGER.debug("getSubstationFullSvg request received with parameter networkUuid = {}, substationID = {}", networkUuid, substationId);

        var parameters = new SingleLineDiagramParameters(useName, centerLabel, diagonalLabel, topologicalColoring, componentLibrary, substationLayout, useFeederPositions);
        Pair<String, String> svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, substationId, parameters);
        String svg = svgAndMetadata.getLeft();
        String metadata = svgAndMetadata.getRight();
        return OBJECT_MAPPER.writeValueAsString(
                OBJECT_MAPPER.createObjectNode()
                        .put("svg", svg)
                        .putRawValue("metadata", new RawValue(metadata)));
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
    @GetMapping(value = "/network-area-diagram/{networkUuid}", produces = IMAGE_SVG_PLUS_XML)
    @Operation(summary = "Get network area diagram image")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The network area diagram svg")})
    public @ResponseBody String getNetworkAreaDiagramSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "Voltage levels ids") @RequestParam(name = "voltageLevelsIds", required = false) List<String> voltageLevelsIds,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "depth") @RequestParam(name = "depth", required = false) int depth) {
        LOGGER.debug("getNetworkAreaDiagramSvg request received with parameter networkUuid = {}, voltageLevelsIds = {}, depth = {}", networkUuid, sanitizeParam(voltageLevelsIds.toString()), depth);

        return networkAeraDiagramService.generateNetworkAreaDiagramSvg(networkUuid, variantId, voltageLevelsIds, depth);
    }
}
