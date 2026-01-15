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
public enum SingleLineDiagramBusinessErrorCode implements BusinessErrorCode {
    EQUIPMENT_NOT_FOUND("singleLineDiagram.equipmentNotFound"),
    MAX_VOLTAGE_LEVELS_DISPLAYED("singleLineDiagram.maxVoltageLevelDisplayed"),
    INVALID_CONFIG_REQUEST("singleLineDiagram.invalidConfigRequest"),
    INVALID_EQUIPMENT("singleLineDiagram.invalidEquipment"),
    INVALID_SUBSTATION_LAYOUT("singleLineDiagram.invalidSubstationLayout"),
    NO_VOLTAGE_LEVEL_ID_PROVIDED("singleLineDiagram.noVoltageLevelIdProvided"),;

    private final String code;

    SingleLineDiagramBusinessErrorCode(String code) {
        this.code = code;
    }

    public String value() {
        return code;
    }
}
