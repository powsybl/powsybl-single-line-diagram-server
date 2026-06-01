/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.iidm.network.*;
import com.powsybl.nad.model.ThreeWtEdge;
import com.powsybl.nad.svg.LabelProviderParameters;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.EdgeInfo;

import java.util.Optional;
import java.util.stream.Stream;

public class NadLabelProvider extends DefaultLabelProvider {
    public NadLabelProvider(Network network, SvgParameters svgParameters) {
        super(
                network,
                new DefaultLabelProvider.EdgeInfoParameters(
                        DefaultLabelProvider.EdgeInfoEnum.REACTIVE_POWER,
                        DefaultLabelProvider.EdgeInfoEnum.EMPTY,
                        DefaultLabelProvider.EdgeInfoEnum.EMPTY,
                        DefaultLabelProvider.EdgeInfoEnum.EMPTY
                ),
                svgParameters.createValueFormatter(),
                new LabelProviderParameters()
        );
    }

    @Override
    public Optional<EdgeInfo> getBranchEdgeInfo(String branchId, String branchType) {
        Branch<?> branch = getNetwork().getBranch(branchId);
        if (branch == null) {
            return Optional.empty();
        }

        double pMax = Stream.of(branch.getTerminal(TwoSides.ONE).getP(), branch.getTerminal(TwoSides.TWO).getP())
            .reduce((a, b) -> Double.compare(Math.abs(a), Math.abs(b)) > 0 ? a : b)
            .orElse(Double.NaN);

        double istMax = getPermanentLimitPercentageMax(branch);
        return Optional.of(new EdgeInfo(
                EdgeInfo.ACTIVE_POWER,
                EdgeInfo.VALUE_PERMANENT_LIMIT_PERCENTAGE,
                branch.getTerminal(TwoSides.ONE).getP(),
                getValueFormatter().formatPower(Math.abs(pMax), ""),
                getValueFormatter().formatPercentage(istMax)
        ));
    }

    @Override
    public Optional<EdgeInfo> getThreeWindingTransformerEdgeInfo(String threeWindingTransformerId, ThreeWtEdge.Side side) {
        ThreeWindingsTransformer twt = getNetwork().getThreeWindingsTransformer(threeWindingTransformerId);
        if (twt == null) {
            return Optional.empty();
        }

        double pMax = Stream.of(twt.getTerminal(ThreeSides.ONE).getP(), twt.getTerminal(ThreeSides.TWO).getP(), twt.getTerminal(ThreeSides.THREE).getP())
            .reduce((a, b) -> Double.compare(Math.abs(a), Math.abs(b)) > 0 ? a : b)
            .orElse(Double.NaN);

        double istMax = getPermanentLimitPercentageMax(twt);
        return Optional.of(new EdgeInfo(
                EdgeInfo.ACTIVE_POWER,
                EdgeInfo.VALUE_PERMANENT_LIMIT_PERCENTAGE,
                twt.getTerminal(ThreeSides.ONE).getP(),
                getValueFormatter().formatPower(Math.abs(pMax), ""),
                getValueFormatter().formatPercentage(istMax)

        ));
    }

    private double getPermanentLimitPercentageMax(Branch<?> branch) {
        return Stream.of(TwoSides.ONE, TwoSides.TWO)
                .map(side -> getPermanentLimitPercentageMax(branch.getTerminal(side), branch.getCurrentLimits(side).orElse(null)))
                .mapToDouble(Double::doubleValue).max().getAsDouble();
    }

    private double getPermanentLimitPercentageMax(ThreeWindingsTransformer transformer) {
        return Stream.of(ThreeSides.ONE, ThreeSides.TWO, ThreeSides.THREE)
                .map(side -> getPermanentLimitPercentageMax(transformer.getTerminal(side), transformer.getLeg(side).getCurrentLimits().orElse(null)))
                .mapToDouble(Double::doubleValue).max().getAsDouble();
    }

    private double getPermanentLimitPercentageMax(Terminal terminal, CurrentLimits currentLimits) {
        return currentLimits != null ? (Math.abs(terminal.getI() * 100) / currentLimits.getPermanentLimit()) : 0;
    }
}
