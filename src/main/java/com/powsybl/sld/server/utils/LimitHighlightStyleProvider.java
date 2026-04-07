/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.utils;

import com.powsybl.iidm.network.Network;
import com.powsybl.sld.model.graphs.Graph;
import com.powsybl.sld.model.nodes.Edge;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.model.nodes.Node;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
public class LimitHighlightStyleProvider extends com.powsybl.sld.svg.styles.iidm.LimitHighlightStyleProvider {

    private final Map<String, String> limitViolationStylesByBranchId;

    public LimitHighlightStyleProvider(Network network, Map<String, String> limitViolationStyles) {
        super(network);
        this.limitViolationStylesByBranchId = limitViolationStyles;
    }

    @Override
    public List<String> getEdgeStyles(Graph graph, Edge edge) {
        List<String> edgeStyleClasses = new ArrayList<>(super.getEdgeStyles(graph, edge));

        // Check custom violations
        if (!limitViolationStylesByBranchId.isEmpty()) {
            getCustomViolationStyle(edge).ifPresent(edgeStyleClasses::add);
        }

        return edgeStyleClasses;
    }

    private Optional<String> getCustomViolationStyle(Edge edge) {
        for (Node node : edge.getNodes()) {
            if (node instanceof FeederNode feederNode) {
                String style = limitViolationStylesByBranchId.get(feederNode.getEquipmentId());
                if (!StringUtils.isBlank(style)) {
                    return Optional.of(style);
                }
            }
        }
        return Optional.empty();
    }
}
