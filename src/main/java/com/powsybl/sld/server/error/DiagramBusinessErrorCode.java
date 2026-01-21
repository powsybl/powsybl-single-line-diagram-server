/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.error;

import com.powsybl.ws.commons.error.BusinessErrorCode;

/**
 * @author Hugo Marcellin <hugo.marcelin at rte-france.com>
 */
public enum DiagramBusinessErrorCode implements BusinessErrorCode {
    EQUIPMENT_NOT_FOUND("diagram.equipmentNotFound"),
    MAX_VOLTAGE_LEVELS_DISPLAYED("diagram.maxVoltageLevelDisplayed"),
    INVALID_CSV("diagram.invalidCsv"),
    INVALID_CONFIG_REQUEST("diagram.invalidConfigRequest"),
    INVALID_EQUIPMENT("diagram.invalidEquipment"),
    INVALID_SUBSTATION_LAYOUT("diagram.invalidSubstationLayout"),
    NO_CONFIGURED_POSITION("diagram.noConfiguredPosition"),
    NO_VOLTAGE_LEVEL_ID_PROVIDED("diagram.noVoltageLevelIdProvided"),;

    private final String code;

    DiagramBusinessErrorCode(String code) {
        this.code = code;
    }

    public String value() {
        return code;
    }
}
