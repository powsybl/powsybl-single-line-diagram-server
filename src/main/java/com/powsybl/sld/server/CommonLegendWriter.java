/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.IdentifiableShortCircuit;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.svg.BusLegendInfo;
import com.powsybl.sld.svg.DefaultSVGLegendWriter;
import com.powsybl.sld.svg.GraphMetadata;
import com.powsybl.sld.svg.SvgParameters;
import com.powsybl.sld.svg.styles.StyleProvider;
import org.springframework.lang.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.DoubleStream;

import static com.powsybl.sld.svg.SVGWriter.GROUP;

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

    /*@Override
    public void drawLegend(VoltageLevelGraph graph, GraphMetadata metadata, StyleProvider styleProvider, Element legendRootElement, double positionX, double positionY) {
        VoltageLevel voltageLevel = network.getVoltageLevel(graph.getVoltageLevelInfos().getId());
        double lowVoltageLimit = voltageLevel.getLowVoltageLimit();
        double highVoltageLimit = voltageLevel.getHighVoltageLimit();
        Double ipMax = getIpMax(voltageLevel);

        Element gNode = legendRootElement.getOwnerDocument().createElement(GROUP);
        gNode.setAttribute("id", voltageLevel.getId());

        Element label = gNode.getOwnerDocument().createElement("text");
//        writeStyleClasses(label, styleProvider.getBusLegendCaptionStyles(caption), StyleClassConstants.BUS_LEGEND_INFO);
        label.setAttribute("id", "test");
        label.setAttribute("x", String.valueOf(positionX));
        label.setAttribute("y", String.valueOf(positionY));
        Text textNode = gNode.getOwnerDocument().createTextNode(String.valueOf(lowVoltageLimit));
        label.appendChild(textNode);
        gNode.appendChild(label);

        legendRootElement.appendChild(gNode);

//        super.drawLegend(graph, metadata, styleProvider, legendRootElement, positionX, positionY);


    }*/

    @Override
    public void drawLegend(
        VoltageLevelGraph graph,
        GraphMetadata metadata,
        StyleProvider styleProvider,
        Element legendRootElement,
        double positionX,
        double positionY
    ) {
        VoltageLevel voltageLevel = network.getVoltageLevel(graph.getVoltageLevelInfos().getId());
        double lowVoltageLimit = voltageLevel.getLowVoltageLimit();
        double highVoltageLimit = voltageLevel.getHighVoltageLimit();
        Double ipMax = getIpMax(voltageLevel);

        Document doc = legendRootElement.getOwnerDocument();
        Element gNode = doc.createElement(GROUP);

        gNode.setAttribute("id", voltageLevel.getId());

        // ---------- foreignObject ----------
        Element foreign = doc.createElementNS("http://www.w3.org/2000/svg", "foreignObject");

        // dimensions du tableau
        foreign.setAttribute("x", String.valueOf(positionX));
        foreign.setAttribute("y", String.valueOf(positionY));
        foreign.setAttribute("width", "220");
        foreign.setAttribute("height", "110");

        // ---------- XHTML table ----------
        Element div = doc.createElementNS("http://www.w3.org/1999/xhtml", "div");
        div.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");

        Element table = doc.createElementNS("http://www.w3.org/1999/xhtml", "table");
        table.setAttribute("class", "legend-table"); // tu pourras la styliser en CSS

        addRow(doc, table, "lowLimit",  String.valueOf(lowVoltageLimit));
        addRow(doc, table, "highLimit", String.valueOf(highVoltageLimit));
        addRow(doc, table, "ipMax",     ipMax != null ? String.valueOf(ipMax) : "-");

        div.appendChild(table);
        foreign.appendChild(div);

        // attach to group
        gNode.appendChild(foreign);
        legendRootElement.appendChild(gNode);

        super.drawLegend(graph, metadata, styleProvider, legendRootElement, positionX + 250, positionY);
    }

    private void addRow(Document doc, Element table, String label, String value) {
        Element tr = doc.createElementNS("http://www.w3.org/1999/xhtml", "tr");

        Element td1 = doc.createElementNS("http://www.w3.org/1999/xhtml", "td");
        td1.appendChild(doc.createTextNode(label));

        Element td2 = doc.createElementNS("http://www.w3.org/1999/xhtml", "td");
        td2.appendChild(doc.createTextNode(value));

        tr.appendChild(td1);
        tr.appendChild(td2);
        table.appendChild(tr);
    }

    private Double getIpMax(@NonNull final VoltageLevel voltageLevel) {
        return Optional.ofNullable((IdentifiableShortCircuit<VoltageLevel>) voltageLevel.getExtension(IdentifiableShortCircuit.class))
            .map(IdentifiableShortCircuit::getIpMax).orElse(null);
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
