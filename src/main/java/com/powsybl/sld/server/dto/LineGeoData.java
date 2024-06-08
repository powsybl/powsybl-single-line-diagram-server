/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto;

import com.powsybl.iidm.network.Country;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Maissa SOUISSI <maissa.souissi at rte-france.com>
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@ToString
public class LineGeoData {

    private String id;

    private Country country1;

    private Country country2;

    String substationStart;

    String substationEnd;

    private List<Coordinate> coordinates = new ArrayList<>();
}
