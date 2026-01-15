/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessExceptionHandler;
import com.powsybl.ws.commons.error.PowsyblWsProblemDetail;
import com.powsybl.ws.commons.error.ServerNameProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */

@ControllerAdvice
public class SingleLineDiagramExceptionHandler extends AbstractBusinessExceptionHandler<SingleLineDiagramBusinessException, SingleLineDiagramBusinessErrorCode> {

    protected SingleLineDiagramExceptionHandler(ServerNameProvider serverNameProvider) {
        super(serverNameProvider);
    }

    @Override
    protected @NonNull SingleLineDiagramBusinessErrorCode getBusinessCode(SingleLineDiagramBusinessException e) {
        return e.getBusinessErrorCode();
    }

    @Override
    protected HttpStatus mapStatus(SingleLineDiagramBusinessErrorCode businessErrorCode) {
        return switch (businessErrorCode) {
            case EQUIPMENT_NOT_FOUND, MAX_VOLTAGE_LEVELS_DISPLAYED, INVALID_CONFIG_REQUEST, INVALID_EQUIPMENT,
                 INVALID_SUBSTATION_LAYOUT,
                 NO_VOLTAGE_LEVEL_ID_PROVIDED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    @ExceptionHandler(SingleLineDiagramBusinessException.class)
    public ResponseEntity<PowsyblWsProblemDetail> handleComputationException(SingleLineDiagramBusinessException exception, HttpServletRequest request) {
        return super.handleDomainException(exception, request);
    }
}

