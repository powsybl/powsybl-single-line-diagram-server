/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.error;

import org.junit.jupiter.api.Test;

import static com.powsybl.sld.server.error.SingleLineDiagramBusinessErrorCode.EQUIPMENT_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
class SingleLineDiagramExceptionTest {

    @Test
    void testMessageAndThrowableConstructor() {
        var cause = new RuntimeException("test");
        var e = new SingleLineDiagramBusinessException(EQUIPMENT_NOT_FOUND, "test", cause);
        assertEquals(EQUIPMENT_NOT_FOUND, e.getBusinessErrorCode());
        assertEquals("test", e.getMessage());
        assertEquals(cause, e.getCause());
    }

    @Test
    void testBusinessErrorCodeConstructor() {
        var e = new SingleLineDiagramBusinessException(EQUIPMENT_NOT_FOUND, "test");
        assertEquals("test", e.getMessage());
        assertEquals(EQUIPMENT_NOT_FOUND, e.getBusinessErrorCode());
    }
}