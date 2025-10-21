/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.sld.server;

import com.powsybl.diagram.util.ValueFormatter;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.nad.model.BusNode;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.DefaultLabelProvider;

/**
 * @author Caroline Jeandat <caroline.jeandat at rte-france.com>
 */

public class NadLabelProvider extends DefaultLabelProvider {
    public NadLabelProvider(Network network, SvgParameters svgParameters) {
        super(network, svgParameters);
    }

    @Override
    public String getBusDescription(BusNode busNode) {
        SvgParameters svgParameters = this.getSvgParameters();
        Network network = this.getNetwork();
        ValueFormatter valueFormatter = this.getValueFormatter();
        if (svgParameters.isBusLegend()) {
            Bus b = network.getBusView().getBus(busNode.getEquipmentId());
            return valueFormatter.formatVoltage(b.getV(), "kV");
        }
        return null;
    }
}
