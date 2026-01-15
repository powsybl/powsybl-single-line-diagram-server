/*
  Copyright (c) 2025, RTE (http://www.rte-france.com)
  This Source Code Form is subject to the terms of the Mozilla Public
  License, v. 2.0. If a copy of the MPL was not distributed with this
  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.error;

/**
 * @author Mohamed Benrejeb <mohamed.benrejeb at rte-france.com>
 */
public class SingleLineDiagramRuntimeException extends RuntimeException {

    public SingleLineDiagramRuntimeException(String message) {
        super(message);
    }

    public SingleLineDiagramRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

}
