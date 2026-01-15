/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.error;

import com.powsybl.ws.commons.error.AbstractBusinessException;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;

import java.util.Map;
import java.util.Objects;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
public class SingleLineDiagramBusinessException extends AbstractBusinessException {

    private final SingleLineDiagramBusinessErrorCode errorCode;
    private final transient Map<String, Object> businessErrorValues;

    @NonNull
    @Override
    public SingleLineDiagramBusinessErrorCode getBusinessErrorCode() {
        return errorCode;
    }

    public SingleLineDiagramBusinessException(SingleLineDiagramBusinessErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.businessErrorValues = Map.of();
    }

    public SingleLineDiagramBusinessException(SingleLineDiagramBusinessErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode);
        this.businessErrorValues = Map.of();
    }

    public SingleLineDiagramBusinessException(SingleLineDiagramBusinessErrorCode errorCode, String message, Map<String, Object> businessErrorValues) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode);
        this.businessErrorValues = businessErrorValues != null ? Map.copyOf(businessErrorValues) : Map.of();
    }

    @NotNull
    @Override
    public Map<String, Object> getBusinessErrorValues() {
        return businessErrorValues;
    }
}
