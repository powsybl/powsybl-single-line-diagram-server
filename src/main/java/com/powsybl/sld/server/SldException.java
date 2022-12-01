/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

/**
 * @author Ben Daamer ahmed<ahmed.bendaamer at rte-france.com>
 */
public class SldException extends RuntimeException {
    public SldException(String msg) {
        super(msg);
    }
}
