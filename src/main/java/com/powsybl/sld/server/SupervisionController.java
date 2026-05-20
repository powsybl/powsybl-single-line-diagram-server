/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Radouane Khouadri <redouane.khouadri_externe at rte-france.com>
 */
@RestController
@RequestMapping(value = "/" + SingleLineDiagramApi.API_VERSION + "/supervision")
@Tag(name = "single-line-diagram-server - Supervision")
@ComponentScan(basePackageClasses = SingleLineDiagramService.class)
public class SupervisionController {

    private final NetworkAreaDiagramService networkAreaDiagramService;

    public SupervisionController(NetworkAreaDiagramService networkAreaDiagramService) {
        this.networkAreaDiagramService = networkAreaDiagramService;
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
