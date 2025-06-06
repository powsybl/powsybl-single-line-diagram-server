/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto.nad;

import com.powsybl.sld.server.utils.ElementType;
import lombok.*;

import java.util.UUID;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ElementParametersInfos {
    private UUID elementUuid;
    private ElementType elementType;
    @Builder.Default
    private Integer depth = 0;
    @Builder.Default
    private Boolean withGeoData = true;
}
