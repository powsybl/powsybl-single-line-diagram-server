/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto;

import com.powsybl.iidm.network.IdentifiableType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author Charly BOUTIER <charly.boutier at rte-france.com>
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Identifiable attributes")
public class IdentifiableAttributes {

    @Schema(description = "identifiable id")
    private String id;

    @Schema(description = "identifiable type")
    private IdentifiableType type;

    @Schema(description = "distribution key")
    private Double distributionKey;
}
