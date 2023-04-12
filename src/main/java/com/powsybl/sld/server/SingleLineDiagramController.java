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
import com.powsybl.sld.server.dto.SvgAndMetadata;
import com.powsybl.sld.server.utils.SldDisplayMode;
import com.powsybl.sld.server.utils.SingleLineDiagramParameters;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
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
    static final String SVG_TAG = "svg";
    static final String METADATA = "metadata";
    static final String ADDITIONAL_METADATA = "additionalMetadata";

    @Autowired
    private SingleLineDiagramService singleLineDiagramService;

    @Autowired
    private NetworkAreaDiagramService networkAreaDiagramService;

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
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String  language) {
        LOGGER.debug("getVoltageLevelSvg request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);
        var parameters =  SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(HORIZONTAL)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, voltageLevelId, parameters).getSvg();
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
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String  language) {
        LOGGER.debug("getVoltageLevelMetadata request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);

        var parameters =  SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(HORIZONTAL)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, voltageLevelId, parameters).getMetadata();
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
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String  language) throws JsonProcessingException {
        LOGGER.debug("getVoltageLevelCompleteSvg request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);
        var parameters =  SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(HORIZONTAL)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, voltageLevelId, parameters);
        String svg = svgAndMetadata.getSvg();
        String metadata = svgAndMetadata.getMetadata();
        Map<String, Object> additionalMetadata = svgAndMetadata.getAdditionalMetadata();
        return OBJECT_MAPPER.writeValueAsString(
                OBJECT_MAPPER.createObjectNode()
                        .put(SVG_TAG, svg)
                        .putRawValue(METADATA, new RawValue(metadata))
                        .putPOJO(ADDITIONAL_METADATA, additionalMetadata));
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
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String  language) {
        LOGGER.debug("getSubstationSvg request received with parameter networkUuid = {}, substationID = {}", networkUuid, substationId);
        var parameters =  SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(substationLayout)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, substationId, parameters).getSvg();
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
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String  language) {
        LOGGER.debug("getSubstationMetadata request received with parameter networkUuid = {}, substationID = {}", networkUuid, substationId);
        var parameters =  SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(substationLayout)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, substationId, parameters).getMetadata();
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
            @Parameter(description = "Sld display mode") @RequestParam(name = "sldDisplayMode", defaultValue = "STATE_VARIABLE") SldDisplayMode sldDisplayMode,
            @Parameter(description = "language") @RequestParam(name = "language", defaultValue = "en") String  language) throws JsonProcessingException {
        LOGGER.debug("getSubstationFullSvg request received with parameter networkUuid = {}, substationID = {}", networkUuid, substationId);
        var parameters =  SingleLineDiagramParameters.builder()
                .useName(useName)
                .labelCentered(centerLabel)
                .diagonalLabel(diagonalLabel)
                .topologicalColoring(topologicalColoring)
                .componentLibrary(componentLibrary)
                .substationLayout(substationLayout)
                .sldDisplayMode(sldDisplayMode)
                .language(language)
                .build();
        SvgAndMetadata svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(networkUuid, variantId, substationId, parameters);
        String svg = svgAndMetadata.getSvg();
        String metadata = svgAndMetadata.getMetadata();
        Map<String, Object> additionalMetadata = svgAndMetadata.getAdditionalMetadata();
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
    @GetMapping(value = "/network-area-diagram/{networkUuid}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get network area diagram image")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "The network area diagram svg")})
    public @ResponseBody String getNetworkAreaDiagramSvg(
            @Parameter(description = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @Parameter(description = "Voltage levels ids") @RequestParam(name = "voltageLevelsIds", required = false) List<String> voltageLevelsIds,
            @Parameter(description = "Variant Id") @RequestParam(name = "variantId", required = false) String variantId,
            @Parameter(description = "depth") @RequestParam(name = "depth", required = false) int depth) throws JsonProcessingException {
        LOGGER.debug("getNetworkAreaDiagramSvg request received with parameter networkUuid = {}, voltageLevelsIds = {}, depth = {}", networkUuid, sanitizeParam(voltageLevelsIds.toString()), depth);
        SvgAndMetadata svgAndMetadata = networkAreaDiagramService.generateNetworkAreaDiagramSvg(networkUuid, variantId, voltageLevelsIds, depth);
        String svg = svgAndMetadata.getSvg();
        Map<String, Object> additionalMetadata = svgAndMetadata.getAdditionalMetadata();
        return OBJECT_MAPPER.writeValueAsString(
                OBJECT_MAPPER.createObjectNode()
                        .put(SVG_TAG, svg)
                        .putPOJO(ADDITIONAL_METADATA, additionalMetadata));
    }
}
