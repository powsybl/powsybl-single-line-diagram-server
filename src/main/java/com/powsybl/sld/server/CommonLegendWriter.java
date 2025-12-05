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
import com.powsybl.sld.svg.*;

import java.util.*;
import java.util.stream.DoubleStream;

public class CommonLegendWriter extends DefaultSVGLegendWriter {
    private static final String UNIT_MW = "MW";
    private static final String UNIT_KV = "kV";
    private static final String UNIT_KA = "kA";

    private static final String PREFIX_VOLTAGE = "U = ";
    private static final String PREFIX_ANGLE = "θ = ";
    private static final String PREFIX_PRODUCTION = "P = ";
    private static final String PREFIX_CONSUMPTION = "C = ";
    private static final String PREFIX_ICC = "ICC = ";

    private static final String KEY_BUS_ID = "busId";
    public static final String KEY_VOLTAGE = "v";
    public static final String KEY_ANGLE = "angle";
    public static final String KEY_CONSUMPTION = "consumption-sum";
    public static final String KEY_PRODUCTION = "production-sum";
    public static final String KEY_ICC = "icc";

    private final Map<String, Double> iccByBusId;

    public static LegendWriterFactory createFactory(Map<String, Double> iccByBusId) {
        return (network, svgParameters) -> new CommonLegendWriter(network, svgParameters, iccByBusId);
    }

    public CommonLegendWriter(Network network, SvgParameters svgParameters, Map<String, Double> iccByBusId) {
        super(network, svgParameters);
        this.iccByBusId = iccByBusId;
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
                    new BusLegendInfo.Caption(PREFIX_CONSUMPTION + formatPowerSum(b.getLoadStream().mapToDouble(l -> l.getTerminal().getP())), KEY_PRODUCTION),
                    new BusLegendInfo.Caption(PREFIX_ICC + getFormattedBusIcc(b.getId()), KEY_ICC)
                ))
            ).toList();
    }

    private String getFormattedBusIcc(String busId) {
        Double iccInA = iccByBusId == null ? null : iccByBusId.get(busId);

        String value = iccInA != null
            ? String.format(Locale.US, "%.1f", iccInA / 1000.0)
            : svgParameters.getUndefinedValueSymbol();

        return value + " " + UNIT_KA;
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
