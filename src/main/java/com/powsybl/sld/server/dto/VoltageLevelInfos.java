/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.VoltageLevel;
import lombok.Getter;

@Getter
public class VoltageLevelInfos extends EquipmentInfos {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String substationId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Country country;

    public VoltageLevelInfos(VoltageLevel voltageLevel) {
        this.id = voltageLevel.getId();
        this.name = voltageLevel.getOptionalName().orElse(null);
        voltageLevel.getSubstation().ifPresent(substation -> {
            this.substationId = substation.getId();
            substation.getCountry().ifPresent(countryValue -> {
                this.country = countryValue;
            });
        });
    }
}
