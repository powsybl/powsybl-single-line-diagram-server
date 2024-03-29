/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.sld.server.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class SvgAndMetadata {

    String svg;

    String metadata;

    Object additionalMetadata;
}
