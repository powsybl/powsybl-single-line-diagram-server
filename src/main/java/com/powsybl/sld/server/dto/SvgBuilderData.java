/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.sld.server.dto;

import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.model.Point;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Builder
@Getter
@Setter
public class SvgBuilderData {

    int scalingFactor;

    VoltageLevelFilter voltageLevelFilter;

    Map<String, Point> positions;
}
