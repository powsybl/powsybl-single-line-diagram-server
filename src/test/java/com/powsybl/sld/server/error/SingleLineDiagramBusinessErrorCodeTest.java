/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.error;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
class SingleLineDiagramBusinessErrorCodeTest {
    @ParameterizedTest
    @EnumSource(DiagramBusinessErrorCode.class)
    void valueMatchesEnumName(DiagramBusinessErrorCode code) {
        assertThat(code.value()).startsWith("diagram.");
    }
}
