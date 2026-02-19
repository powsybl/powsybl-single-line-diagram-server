/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.diagram.util.PermanentLimitPercentageMax;
import com.powsybl.iidm.network.*;
import com.powsybl.nad.model.ThreeWtEdge;
import com.powsybl.nad.svg.EdgeInfo;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.DefaultLabelProvider;

import java.util.Optional;
import java.util.stream.Stream;

public class NadLabelProvider1 extends DefaultLabelProvider {

    public NadLabelProvider1(Network network, SvgParameters svgParameters) {
        super(network, svgParameters);
     }

    @Override
    public Optional<EdgeInfo> getBranchEdgeInfo(String branchId, String branchType) {
         Branch<?> branch = getNetwork().getBranch(branchId);
        if (branch == null) {
            return Optional.empty();
        }

         double pMax = Math.max(Math.abs(branch.getTerminal(TwoSides.ONE).getP()),
            Math.abs(branch.getTerminal(TwoSides.TWO).getP()));

        double istMax = PermanentLimitPercentageMax.getPermanentLimitPercentageMax(branch);

        return Optional.of(new EdgeInfo(
            EdgeInfo.ACTIVE_POWER,                         // gauche de la flèche → puissance max
            EdgeInfo.VALUE_PERMANENT_LIMIT_PERCENTAGE,    // droite de la flèche → % IST max
            pMax,                                         // valeur pour la flèche (sens)
            getValueFormatter().formatPower(pMax, ""),  // label à gauche
            getValueFormatter().formatPercentage(istMax)  // label à droite
        ));
    }

    @Override
    public Optional<EdgeInfo> getThreeWindingTransformerEdgeInfo(String threeWindingTransformerId, ThreeWtEdge.Side side) {
        ThreeWindingsTransformer twt = getNetwork().getThreeWindingsTransformer(threeWindingTransformerId);
        if (twt == null) {
            return Optional.empty();
        }

        double pMax = Stream.of(ThreeSides.ONE, ThreeSides.TWO, ThreeSides.THREE)
            .mapToDouble(s -> Math.abs(twt.getTerminal(s).getP()))
            .max()
            .orElse(Double.NaN);

        double istMax = PermanentLimitPercentageMax.getPermanentLimitPercentageMax(twt);

        return Optional.of(new EdgeInfo(
            EdgeInfo.ACTIVE_POWER,
            EdgeInfo.VALUE_PERMANENT_LIMIT_PERCENTAGE,
            pMax,
            getValueFormatter().formatPower(pMax, "MW"),
            getValueFormatter().formatPercentage(istMax)
        ));
    }
}

