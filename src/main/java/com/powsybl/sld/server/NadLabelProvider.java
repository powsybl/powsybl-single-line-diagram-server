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
import com.powsybl.nad.svg.iidm.DefaultLabelProvider;
import com.powsybl.nad.svg.EdgeInfo;
import com.powsybl.diagram.util.PermanentLimitPercentageMax;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

public class NadLabelProvider extends DefaultLabelProvider {
    private final SvgParameters svgParameters;
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
        this.svgParameters = svgParameters;
    }

    @Override
    public Optional<EdgeInfo> getBranchEdgeInfo(String branchId, String branchType) {
        Branch<?> branch = getNetwork().getBranch(branchId);
        if (branch == null) {
            return Optional.empty();
        }

         double pMax = Math.max(
            Math.abs(branch.getTerminal(TwoSides.ONE).getP()),
            Math.abs(branch.getTerminal(TwoSides.TWO).getP())
        );

        // IST max
        double istMax = getPermanentLimitPercentageMax(branch);
        return Optional.of(new EdgeInfo(
            EdgeInfo.ACTIVE_POWER,
            EdgeInfo.VALUE_PERMANENT_LIMIT_PERCENTAGE,
            pMax,
            getValueFormatter().formatPower(pMax, "MW"),
            getValueFormatter().formatPercentage(istMax)
            ));
    }

    private String getFormattedPermanentLimit(double percentage) {
        return Double.isNaN(percentage)
            ?            svgParameters.getUndefinedValueSymbol()

            : String.format(Locale.US, "%.0f%%", percentage);
    }

    @Override
    public Optional<EdgeInfo> getThreeWindingTransformerEdgeInfo(String threeWindingTransformerId, ThreeWtEdge.Side side) {
        ThreeWindingsTransformer twt = getNetwork().getThreeWindingsTransformer(threeWindingTransformerId);
        if (twt == null) {
            return Optional.empty();
        }

        double maxActivePower = Stream.of(ThreeSides.ONE, ThreeSides.TWO, ThreeSides.THREE)
            .mapToDouble(s -> Math.abs(twt.getTerminal(s).getP()))
            .max()
            .orElse(Double.NaN);

        double istMax = getPermanentLimitPercentageMax(twt);


        return Optional.of(new EdgeInfo(
            EdgeInfo.ACTIVE_POWER,
            EdgeInfo.VALUE_PERMANENT_LIMIT_PERCENTAGE,
            maxActivePower,

            getValueFormatter().formatPower(maxActivePower, "MW"),
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
