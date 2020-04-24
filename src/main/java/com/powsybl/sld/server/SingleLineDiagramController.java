/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import io.swagger.annotations.*;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.RawValue;

import javax.inject.Inject;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + SingleLineDiagramApi.API_VERSION + "/")
@Api(tags = "single-line-diagram-server")
@ComponentScan(basePackageClasses = SingleLineDiagramService.class)
public class SingleLineDiagramController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleLineDiagramController.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static final String IMAGE_SVG_PLUS_XML = "image/svg+xml";
    static final String APPLICATION_ZIP = "application/zip";

    @Inject
    private SingleLineDiagramService singleLineDiagramService;

    @GetMapping(value = "/svg/{networkUuid}/{voltageLevelId}", produces = IMAGE_SVG_PLUS_XML)
    @ApiOperation(value = "Get voltage level image", response = StreamingResponseBody.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The voltage level SVG")})
    public @ResponseBody String getVoltageLevelSvg(
            @ApiParam(value = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @ApiParam(value = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId,
            @ApiParam(value = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @ApiParam(value = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @ApiParam(value = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @ApiParam(value = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring) {
        LOGGER.debug("getVoltageLevelSvg request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);
        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, voltageLevelId, useName, centerLabel, diagonalLabel, topologicalColoring).getLeft();
    }

    @GetMapping(value = "/metadata/{networkUuid}/{voltageLevelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get the voltage level svg metadata", response = StreamingResponseBody.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The voltage level SVG metadata")})
    public @ResponseBody String getVoltageLevelMetadata(
            @ApiParam(value = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @ApiParam(value = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId,
            @ApiParam(value = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @ApiParam(value = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @ApiParam(value = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @ApiParam(value = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring) {
        LOGGER.debug("getVoltageLevelMetadata request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);

        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, voltageLevelId, useName, centerLabel, diagonalLabel, topologicalColoring).getRight();
    }

    @GetMapping(value = "svg-and-metadata/{networkUuid}/{voltageLevelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get voltage level svg and metadata", response = StreamingResponseBody.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The voltage level svg and metadata")})
    public @ResponseBody String getVoltageLevelFullSvg(
            @ApiParam(value = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @ApiParam(value = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId,
            @ApiParam(value = "useName") @RequestParam(name = "useName", defaultValue = "false") boolean useName,
            @ApiParam(value = "centerLabel") @RequestParam(name = "centerLabel", defaultValue = "false") boolean centerLabel,
            @ApiParam(value = "diagonalLabel") @RequestParam(name = "diagonalLabel", defaultValue = "false") boolean diagonalLabel,
            @ApiParam(value = "topologicalColoring") @RequestParam(name = "topologicalColoring", defaultValue = "false") boolean topologicalColoring)
        throws JsonProcessingException {
        LOGGER.debug("getVoltageLevelCompleteSvg request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);

        Pair<String, String> svgAndMetadata = singleLineDiagramService.generateSvgAndMetadata(networkUuid, voltageLevelId, useName, centerLabel, diagonalLabel, topologicalColoring);
        String svg = svgAndMetadata.getLeft();
        String metadata = svgAndMetadata.getRight();
        return OBJECT_MAPPER.writeValueAsString(
                OBJECT_MAPPER.createObjectNode()
                    .put("svg", svg)
                    .putRawValue("metadata", new RawValue(metadata)));
    }
}
