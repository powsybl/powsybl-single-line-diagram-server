/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.error;

import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Map;

import static com.powsybl.sld.server.error.SingleLineDiagramBusinessErrorCode.INVALID_CONFIG_REQUEST;
import static com.powsybl.sld.server.error.SingleLineDiagramBusinessErrorCode.MAX_VOLTAGE_LEVELS_DISPLAYED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */

class SingleLineDiagramExceptionHandlerTest {

    private SingleLineDiagramExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SingleLineDiagramExceptionHandler(() -> "single-line-diagram");
    }

    @Test
    void mapsBusinessErrorValues() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sld");
        SingleLineDiagramBusinessException exception = new SingleLineDiagramBusinessException(MAX_VOLTAGE_LEVELS_DISPLAYED, "max voltage levels displayed reached", Map.of("nbVoltageLevels", 12, "maxVoltageLevels", 10));
        ResponseEntity<PowsyblWsProblemDetail> response = handler.handleComputationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertEquals("singleLineDiagram.maxVoltageLevelDisplayed", response.getBody().getBusinessErrorCode());
        assertEquals(12, response.getBody().getBusinessErrorValues().get("nbVoltageLevels"));
        assertEquals(10, response.getBody().getBusinessErrorValues().get("maxVoltageLevels"));
    }

    @Test
    void mapsBadRequestBusinessErrorToStatus() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/sld");
        SingleLineDiagramBusinessException exception = new SingleLineDiagramBusinessException(INVALID_CONFIG_REQUEST, "Invalid config request");
        ResponseEntity<PowsyblWsProblemDetail> response = handler.handleComputationException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertEquals("singleLineDiagram.invalidConfigRequest", response.getBody().getBusinessErrorCode());
    }
}

