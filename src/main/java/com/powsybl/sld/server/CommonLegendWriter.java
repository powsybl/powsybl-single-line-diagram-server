/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.svg.BusLegendInfo;
import com.powsybl.sld.svg.DefaultSVGLegendWriter;
import com.powsybl.sld.svg.SvgParameters;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.DoubleStream;

public class CommonLegendWriter extends DefaultSVGLegendWriter {
    private static final String UNIT_MW = "MW";
    private static final String UNIT_KV = "kV";

    private static final String PREFIX_VOLTAGE = "U = ";
    private static final String PREFIX_ANGLE = "θ = ";
    private static final String PREFIX_PRODUCTION = "P = ";
    private static final String PREFIX_CONSUMPTION = "C = ";

    private static final String KEY_BUS_ID = "busId";
    public static final String KEY_VOLTAGE = "v";
    public static final String KEY_ANGLE = "angle";
    public static final String KEY_CONSUMPTION = "consumption-sum";
    public static final String KEY_PRODUCTION = "production-sum";

    public CommonLegendWriter(Network network, SvgParameters svgParameters) {
        super(network, svgParameters);
    }

    @Override
    protected List<BusLegendInfo> getBusLegendInfos(VoltageLevelGraph graph) {
        VoltageLevel vl = network.getVoltageLevel(graph.getVoltageLevelInfos().getId());
        return vl.getBusView().getBusStream()
            .map(b ->
                new BusLegendInfo(b.getId(), List.of(
                    new BusLegendInfo.Caption(b.getId(), KEY_BUS_ID),
                    new BusLegendInfo.Caption(PREFIX_VOLTAGE + valueFormatter.formatVoltage(b.getV(), UNIT_KV), KEY_VOLTAGE),
                    new BusLegendInfo.Caption(PREFIX_ANGLE + valueFormatter.formatAngleInDegrees(b.getAngle()), KEY_ANGLE),
                    new BusLegendInfo.Caption(PREFIX_PRODUCTION + formatPowerSum(b.getGeneratorStream().mapToDouble(g -> g.getTerminal().getP())), KEY_CONSUMPTION),
                    new BusLegendInfo.Caption(PREFIX_CONSUMPTION + formatPowerSum(b.getLoadStream().mapToDouble(l -> l.getTerminal().getP())), KEY_PRODUCTION)
                ))
            ).toList();
    }

    private String formatPowerSum(DoubleStream stream) {
        OptionalDouble sum = sumDoubleStream(stream);
        String value = sum.isPresent()
            ? String.valueOf(Math.round(Math.abs(sum.getAsDouble())))
            : svgParameters.getUndefinedValueSymbol();
        return value + " " + UNIT_MW;
    }

    private static OptionalDouble sumDoubleStream(DoubleStream stream) {
        DoubleSummaryStatistics stats = stream.summaryStatistics();
        return stats.getCount() == 0
            ? OptionalDouble.empty()
            : OptionalDouble.of(stats.getSum());
    }
}
