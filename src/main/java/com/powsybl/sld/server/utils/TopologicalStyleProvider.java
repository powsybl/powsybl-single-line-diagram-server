/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.utils;

import com.powsybl.commons.config.BaseVoltagesConfig;
import com.powsybl.iidm.network.Network;
import com.powsybl.nad.model.BranchEdge;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
public class TopologicalStyleProvider extends com.powsybl.nad.svg.iidm.TopologicalStyleProvider {

    private final Map<String, String> limitViolationStylesByBranchId;

    public TopologicalStyleProvider(Network network, BaseVoltagesConfig baseVoltageStyle, Map<String, String> limitViolationStyles) {
        super(network, baseVoltageStyle);
        this.limitViolationStylesByBranchId = limitViolationStyles;
    }

    @Override
    public List<String> getBranchEdgeStyleClasses(BranchEdge branchEdge) {
        List<String> branchEdgeStyleClasses = super.getBranchEdgeStyleClasses(branchEdge);

        // Check custom violations
        if (!limitViolationStylesByBranchId.isEmpty()) {
            String customStyle = limitViolationStylesByBranchId.get(branchEdge.getEquipmentId());
            if (!StringUtils.isBlank(customStyle)) {
                branchEdgeStyleClasses.add(customStyle);
            }
        }

        return branchEdgeStyleClasses;
    }
}
