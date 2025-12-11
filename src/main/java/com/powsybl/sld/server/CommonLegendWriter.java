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
import com.powsybl.sld.svg.*;
import com.powsybl.sld.svg.styles.StyleClassConstants;
import com.powsybl.sld.svg.styles.StyleProvider;
import com.powsybl.sld.util.IdUtil;
import lombok.NonNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.OptionalDouble;
import java.util.*;
import java.util.Optional;
import java.util.stream.DoubleStream;

import static com.powsybl.diagram.util.CssUtil.writeStyleClasses;
import static com.powsybl.sld.svg.SVGWriter.GROUP;

public class CommonLegendWriter extends DefaultSVGLegendWriter {
    private static final String UNIT_MW = "MW";
    private static final String UNIT_KV = "kV";
    private static final String UNIT_KA = "kA";

    public static final String KEY_VOLTAGE = "v";
    public static final String KEY_ANGLE = "angle";
    public static final String KEY_CONSUMPTION = "consumption-sum";
    public static final String KEY_PRODUCTION = "production-sum";
    public static final String KEY_ICC = "icc";

    private final Map<String, Double> iccByBusId;
    private static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

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
                    new BusLegendInfo.Caption(valueFormatter.formatVoltage(b.getV(), UNIT_KV), KEY_VOLTAGE),
                    new BusLegendInfo.Caption(valueFormatter.formatAngleInDegrees(b.getAngle()), KEY_ANGLE),
                    new BusLegendInfo.Caption(formatPowerSum(b.getGeneratorStream().mapToDouble(g -> g.getTerminal().getP())), KEY_CONSUMPTION),
                    new BusLegendInfo.Caption(formatPowerSum(b.getLoadStream().mapToDouble(l -> l.getTerminal().getP())), KEY_PRODUCTION),
                    new BusLegendInfo.Caption(getFormattedBusIcc(b.getId()), KEY_ICC)
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
        Element foreign = doc.createElementNS(SVG_NS, "foreignObject");

        // dimensions du tableau
        foreign.setAttribute("x", String.valueOf(positionX));
        foreign.setAttribute("y", String.valueOf(positionY));
        foreign.setAttribute("width", "1000"); //TODO: to calculate ?
        foreign.setAttribute("height", "200");

        // ---------- XHTML table ----------
        Element div = doc.createElementNS(XHTML_NS, "div");
        div.setAttribute("class", "legend-root");

        Element voltageLevelTableDiv = doc.createElementNS(XHTML_NS, "div");
        voltageLevelTableDiv.setAttribute("class", "legend-block");

        Element title = doc.createElementNS(XHTML_NS, "div");
        title.setAttribute("class", "legend-title");

        Element name = doc.createElementNS(XHTML_NS, "span");
        name.setTextContent("⦿ " + voltageLevel.getId());

        title.appendChild(name);

        Element table = doc.createElementNS(XHTML_NS, "table");
        table.setAttribute("class", "legend-table"); // tu pourras la styliser en CSS

        addRow(doc, table, "Umin", valueFormatter.formatVoltage(lowVoltageLimit, UNIT_KV));
        addRow(doc, table, "Uman", valueFormatter.formatVoltage(highVoltageLimit, UNIT_KV));
        addRow(doc, table, "IMACC", valueFormatter.formatCurrent(ipMax != null ? ipMax / 1000 : null, UNIT_KA));

        voltageLevelTableDiv.appendChild(title);
        voltageLevelTableDiv.appendChild(table);
        div.appendChild(voltageLevelTableDiv);
        foreign.appendChild(div);

        // attach to group
        gNode.appendChild(foreign);
        legendRootElement.appendChild(gNode);

        keepDrawingLegend(graph, metadata, styleProvider, div);
    }

    private void addRow(Document doc, Element table, String label, String value) {
        Element tr = doc.createElementNS(XHTML_NS, "tr");

        Element td1 = doc.createElementNS(XHTML_NS, "td");
        td1.appendChild(doc.createTextNode(label));

        Element td2 = doc.createElementNS(XHTML_NS, "td");
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

    public void keepDrawingLegend(VoltageLevelGraph graph, GraphMetadata metadata, StyleProvider styleProvider, Element legendDiv) {
        Document doc = legendDiv.getOwnerDocument();
        // legend-root = display:flex → bus-block côte à côte

        //
        // ---- GET ALL BUS LEGENDS ----
        //
        List<BusLegendInfo> buses = getBusLegendInfos(graph);

        for (BusLegendInfo bus : buses) {
            String unescapedIdNode = metadata.getSvgParameters().getPrefixId() + "NODE_" + bus.busId();
            String idNode = IdUtil.escapeId(unescapedIdNode);
            //
            // ---- ONE BUS BLOCK ----
            //
            Element block = doc.createElementNS(XHTML_NS, "div");
            block.setAttribute("class", "legend-block");
            block.setAttribute("id", idNode);

            //
            // ---- TITLE (circle + bus name) ----
            //
            Element title = doc.createElementNS(XHTML_NS, "div");
            title.setAttribute("class", "legend-title");

            Element circle = doc.createElementNS(XHTML_NS, "div");
            circle.setAttribute("id", IdUtil.escapeId(unescapedIdNode + "_circle"));
            circle.appendChild(doc.createTextNode(" "));

            List<String> circleClasses = styleProvider.getBusStyles(bus.busId(), graph);
            circleClasses.add("bus-circle");
            writeStyleClasses(circle, circleClasses);

            Element name = doc.createElementNS(XHTML_NS, "span");
            name.setTextContent(bus.busId());

            title.appendChild(circle);
            title.appendChild(name);

            //
            // ---- TABLE OF THIS BUS ----
            //
            Element table = doc.createElementNS(XHTML_NS, "table");
            table.setAttribute("class", "legend-table");

            for (BusLegendInfo.Caption caption : bus.captions()) {
                Element tr = doc.createElementNS(XHTML_NS, "tr");
                writeStyleClasses(tr, styleProvider.getBusLegendCaptionStyles(caption), StyleClassConstants.BUS_LEGEND_INFO);
                tr.setAttribute("id", IdUtil.escapeId(idNode + "_" + caption.type()));

                Element tdKey = doc.createElementNS(XHTML_NS, "td");
                tdKey.setAttribute("class", "label-cell");
                tdKey.setTextContent(getCaptionTypeColumnLabel(caption));

                Element tdVal = doc.createElementNS(XHTML_NS, "td");
                tdVal.setAttribute("class", "value-cell");
                tdVal.setTextContent(caption.label());

                tr.appendChild(tdKey);
                tr.appendChild(tdVal);
                table.appendChild(tr);
            }

            block.appendChild(title);
            block.appendChild(table);
            legendDiv.appendChild(block);
        }
    }

    private String getCaptionTypeColumnLabel(BusLegendInfo.Caption caption) {
        return switch (caption.type()) {
            case KEY_VOLTAGE -> "U";
            case KEY_ANGLE -> "θ";
            case KEY_PRODUCTION -> "P";
            case KEY_CONSUMPTION -> "C";
            case KEY_ICC -> "ICC";
            default -> "";
        };
    }
}
