/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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

    @Inject
    private SingleLineDiagramService singleLineDiagramService;

    @GetMapping(value = "/svg/{networkUuid}/{voltageLevelId}", produces = MediaType.APPLICATION_XML_VALUE)
    @ApiOperation(value = "Get voltage level image", response = StreamingResponseBody.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The voltage level SVG")})
    public @ResponseBody String getVoltageLevelSvg(
            @ApiParam(value = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @ApiParam(value = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId) {
        LOGGER.debug("getVoltageLevelSvg request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);

        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, voltageLevelId).getLeft();
    }

    @GetMapping(value = "/metadata/{networkUuid}/{voltageLevelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get the voltage level svg metadata", response = StreamingResponseBody.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The voltage level SVG metadata")})
    public @ResponseBody String getVoltageLevelMetadata(
            @ApiParam(value = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @ApiParam(value = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId) {
        LOGGER.debug("getVoltageLevelMetadata request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);

        return singleLineDiagramService.generateSvgAndMetadata(networkUuid, voltageLevelId).getRight();
    }

    @GetMapping(value = "svg-and-metadata/{networkUuid}/{voltageLevelId}", produces = "application/zip")
    @ApiOperation(value = "Get voltage level svg and metadata", response = StreamingResponseBody.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The voltage level svg and metadata")})
    public @ResponseBody byte[] getVoltageLevelFullSvg(
            @ApiParam(value = "Network UUID") @PathVariable("networkUuid") UUID networkUuid,
            @ApiParam(value = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelId) {
        LOGGER.debug("getVoltageLevelCompleteSvg request received with parameter networkUuid = {}, voltageLevelID = {}", networkUuid, voltageLevelId);

        return singleLineDiagramService.generateSvgAndMetadataZip(networkUuid, voltageLevelId);
    }
}
