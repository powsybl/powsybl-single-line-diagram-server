/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.OperatingStatus;
import com.powsybl.nad.model.ThreeWtEdge;
import com.powsybl.nad.svg.*;
import com.powsybl.nad.svg.iidm.DefaultLabelProvider;

import java.util.Optional;
import java.util.stream.Stream;

public class NadLabelProvider extends DefaultLabelProvider {
    private static final String PLANNED_OUTAGE_BRANCH_NODE_DECORATOR = "LOCK";
    private static final String FORCED_OUTAGE_BRANCH_NODE_DECORATOR = "FLASH";

    public NadLabelProvider(Network network, SvgParameters svgParameters) {
        super(
                network,
                svgParameters.createValueFormatter(),
                new LabelProviderParameters()
                        .setEdgeInfoParameters(
                                new EdgeInfoParameters(
                                        EdgeInfoEnum.REACTIVE_POWER,
                                        EdgeInfoEnum.EMPTY,
                                        EdgeInfoEnum.EMPTY,
                                        EdgeInfoEnum.EMPTY
                                )
                        )

        );
        this.setDisplayWithAbs(true);
        this.setDisplayAngle(false);
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
        double pMaxRef = Math.max(
                branch.getTerminal(TwoSides.ONE).getP(),
                branch.getTerminal(TwoSides.TWO).getP()
        );

        double istMax = getPermanentLimitPercentageMax(branch);
        String operatingStatusDecorator = (!branch.getTerminal1().isConnected() && !branch.getTerminal2().isConnected())
                ? getOperatingStatusDecorator(branch)
                : null;

        return Optional.of(new EdgeInfo(
                EdgeInfo.ACTIVE_POWER,
                EdgeInfo.VALUE_PERMANENT_LIMIT_PERCENTAGE,
                pMaxRef,
                getValueFormatter().formatPower(pMax, ""),
                getValueFormatter().formatPercentage(istMax), operatingStatusDecorator
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

        double pMaxRef = Stream.of(ThreeSides.ONE, ThreeSides.TWO, ThreeSides.THREE)
                .mapToDouble(s -> twt.getTerminal(s).getP())
                .max()
                .orElse(Double.NaN);

        double istMax = getPermanentLimitPercentageMax(twt);

        String operatingStatusDecorator = (!twt.getLeg1().getTerminal().isConnected() && !twt.getLeg2().getTerminal().isConnected() && !twt.getLeg3().getTerminal().isConnected())
                ? getOperatingStatusDecorator(twt)
                : null;

        return Optional.of(new EdgeInfo(
                EdgeInfo.ACTIVE_POWER,
                EdgeInfo.VALUE_PERMANENT_LIMIT_PERCENTAGE,
                pMaxRef,
                getValueFormatter().formatPower(pMax, ""),
                getValueFormatter().formatPercentage(istMax), operatingStatusDecorator
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

    private <T extends Identifiable<T>> String getOperatingStatusDecorator(Identifiable<T> identifiable) {
        OperatingStatus<T> operatingStatus = identifiable.getExtension(OperatingStatus.class);
        if (operatingStatus != null) {
            return switch (operatingStatus.getStatus()) {
                case PLANNED_OUTAGE -> PLANNED_OUTAGE_BRANCH_NODE_DECORATOR;
                case FORCED_OUTAGE -> FORCED_OUTAGE_BRANCH_NODE_DECORATOR;
                case IN_OPERATION -> null;
            };
        }
        return null;
    }
}
