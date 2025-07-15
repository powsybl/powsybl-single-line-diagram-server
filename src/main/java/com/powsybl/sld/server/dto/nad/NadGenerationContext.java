/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto.nad;

import com.powsybl.iidm.network.Network;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Charly Boutier <charly.boutier at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class NadGenerationContext {

    private Network network;
    private UUID networkUuid;
    private String variantId;
    private boolean shouldFetchGeoData;
    private Integer scalingFactor;

    @Builder.Default
    private List<NadVoltageLevelPositionInfos> positions = new ArrayList<>();

    @Builder.Default
    private List<String> voltageLevelIds = new ArrayList<>();

    private VoltageLevelFilter voltageLevelFilter;

    private NadParameters nadParameters;
}
