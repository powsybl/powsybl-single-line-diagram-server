/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.utils;

import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TopologyKind;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.BusbarSectionPosition;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.sld.layout.*;
import com.powsybl.sld.layout.position.clustering.PositionByClustering;
import com.powsybl.sld.layout.position.predefined.PositionPredefined;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;

import java.util.Objects;

/**
 * @author Kevin Le Saulnier <kevin.le-saulnier at rte-france.com>
 */
//TODO: to remove once SmartVoltageLevelLayoutFactory allow us to pass PositionVoltageLevelLayoutFactoryParameters to VoltageLevelLayoutFactory
public class CustomVoltageLevelLayoutFactory implements VoltageLevelLayoutFactory {
    private final Network network;

    public CustomVoltageLevelLayoutFactory(Network network) {
        this.network = Objects.requireNonNull(network);
    }

    @Override
    public Layout create(VoltageLevelGraph graph) {
        VoltageLevel vl = network.getVoltageLevel(graph.getVoltageLevelInfos().getId());
        return selectVoltageLevelLayoutFactory(vl).create(graph);
    }

    private VoltageLevelLayoutFactory selectVoltageLevelLayoutFactory(VoltageLevel vl) {
        PositionVoltageLevelLayoutFactoryParameters positionVoltageLevelLayoutFactoryParameters = defaultPositionVoltageLevelLayoutFactoryParameters();

        return vl.getTopologyKind() == TopologyKind.BUS_BREAKER || hasAtLeastOneExtension(vl)
            ? new PositionVoltageLevelLayoutFactory(new PositionPredefined(), positionVoltageLevelLayoutFactoryParameters)
            : new PositionVoltageLevelLayoutFactory(new PositionByClustering(), positionVoltageLevelLayoutFactoryParameters);
    }

    private PositionVoltageLevelLayoutFactoryParameters defaultPositionVoltageLevelLayoutFactoryParameters() {
        return new PositionVoltageLevelLayoutFactoryParameters().setSubstituteInternalMiddle2wtByEquipmentNodes(false);
    }

    private static boolean hasAtLeastOneExtension(VoltageLevel vl) {
        // check for position extensions
        for (Connectable c : vl.getConnectables()) {
            if (c.getExtension(ConnectablePosition.class) != null
                || c.getExtension(BusbarSectionPosition.class) != null) {
                return true;
            }
        }
        return false;
    }
}

