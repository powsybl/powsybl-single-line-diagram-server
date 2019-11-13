package com.powsybl.single.line.diagram.server;

import com.powsybl.voltage.level.diagram.model.VoltageLevelDiagramApi;
import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.inject.Inject;
import java.io.*;

@RestController
@RequestMapping(value = "/" + VoltageLevelDiagramApi.API_VERSION + "/voltage-level-diagram-server")
@Api(tags = "voltage-level-diagram-server")
@ComponentScan(basePackageClasses = SingleLineDiagramService.class)
public class SingleLineDiagramController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleLineDiagramController.class);

    @Inject
    private SingleLineDiagramService singleLineDiagramService;

    @GetMapping(value = "/svg/{networkId}/{voltageLevelId}", produces = MediaType.APPLICATION_XML_VALUE)
    @ApiOperation(value = "Get voltage level image", response = StreamingResponseBody.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The voltage level SVG")})
    public @ResponseBody ResponseEntity<StreamingResponseBody> getVoltageLevelSvg(
            @ApiParam(value = "Network ID") @PathVariable("networkId") String networkId,
            @ApiParam(value = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelID) {
        LOGGER.debug("getVoltageLevelSvg request received with parameter networkId = {}, voltageLevelID = {}", networkId, voltageLevelID);

        return singleLineDiagramService.getVoltageLevelSvg(networkId, voltageLevelID);
    }

    @GetMapping(value = "/metadata/{networkId}/{voltageLevelId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Get the voltage level svg metadata", response = StreamingResponseBody.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The voltage level SVG metadata")})
    public @ResponseBody ResponseEntity<StreamingResponseBody> getVoltageLevelMetadata(
            @ApiParam(value = "Network ID") @PathVariable("networkId") String networkId,
            @ApiParam(value = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelID) {
        LOGGER.debug("getVoltageLevelMetadata request received with parameter networkId = {}, voltageLevelID = {}", networkId, voltageLevelID);

        return singleLineDiagramService.getVoltageLevelMetadata(networkId, voltageLevelID);
    }

    @GetMapping(value = "svg-and-metadata/{networkId}/{voltageLevelId}", produces = "application/zip")
    @ApiOperation(value = "Get voltage level svg and metadata", response = StreamingResponseBody.class)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "The voltage level svg and metadata")})
    public @ResponseBody byte[] getVoltageLevelFullSvg(
            @ApiParam(value = "Network ID") @PathVariable("networkId") String networkId,
            @ApiParam(value = "VoltageLevel ID") @PathVariable("voltageLevelId") String voltageLevelID) throws IOException {
        LOGGER.debug("getVoltageLevelCompleteSvg request received with parameter networkId = {}, voltageLevelID = {}", networkId, voltageLevelID);

        return singleLineDiagramService.getVoltageLevelCompleteSvg(networkId, voltageLevelID);
    }
}
