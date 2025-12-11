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
import static com.powsybl.sld.svg.styles.StyleClassConstants.BUS_LEGEND_INFO;

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
        double x,
        double y
    ) {
        VoltageLevel vl = network.getVoltageLevel(graph.getVoltageLevelInfos().getId());
        Document doc = legendRootElement.getOwnerDocument();

        // create foreign block that will contain the whole HTML legend
        Element foreign = createForeignObject(doc, x, y, 2000, 300);

        // create flew div that will contain each block (voltage level and buses)
        Element legendRoot = doc.createElementNS(XHTML_NS, "div");
        legendRoot.setAttribute("class", "legend-root");

        // create voltage level legend block
        Element leftBlock = createVoltageLevelBlock(doc, vl);
        legendRoot.appendChild(leftBlock);

        // create legend block for each bus
        addBusBlocks(legendRoot, graph, metadata, styleProvider);

        foreign.appendChild(legendRoot);
        legendRootElement.appendChild(foreign);
    }

    private Double getIpMax(@NonNull final VoltageLevel voltageLevel) {
        return Optional.ofNullable((IdentifiableShortCircuit<VoltageLevel>) voltageLevel.getExtension(IdentifiableShortCircuit.class))
            .map(IdentifiableShortCircuit::getIpMax).orElse(Double.NaN);
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

    // Methods to draw HTML legend
    private Element createForeignObject(Document doc, double x, double y, int width, int height) {
        Element foreign = doc.createElementNS(SVG_NS, "foreignObject");

        foreign.setAttribute("x", String.valueOf(x));
        foreign.setAttribute("y", String.valueOf(y));
        foreign.setAttribute("width", String.valueOf(width));
        foreign.setAttribute("height", String.valueOf(height));

        return foreign;
    }

    private Element createVoltageLevelBlock(Document doc, VoltageLevel vl) {
        Element block = doc.createElementNS(XHTML_NS, "div");
        block.setAttribute("class", "legend-block");

        Element title = createTitle(doc, "⦿ " + vl.getId(), null);
        block.appendChild(title);

        Element table = doc.createElementNS(XHTML_NS, "table");
        table.setAttribute("class", "legend-table");

        addRow(doc, table, "Umin", valueFormatter.formatVoltage(vl.getLowVoltageLimit(), UNIT_KV));
        addRow(doc, table, "Umax", valueFormatter.formatVoltage(vl.getHighVoltageLimit(), UNIT_KV));
        addRow(doc, table, "IMACC", valueFormatter.formatCurrent(getIpMax(vl), UNIT_KA));

        block.appendChild(table);
        return block;
    }

    private void addBusBlocks(
        Element legendRoot,
        VoltageLevelGraph graph,
        GraphMetadata metadata,
        StyleProvider styleProvider
    ) {
        Document doc = legendRoot.getOwnerDocument();

        for (BusLegendInfo bus : getBusLegendInfos(graph)) {
            String baseId = metadata.getSvgParameters().getPrefixId() + "NODE_" + bus.busId();
            String escapedId = IdUtil.escapeId(baseId);

            Element block = createBusBlock(doc, bus, escapedId, styleProvider, graph);
            legendRoot.appendChild(block);
        }
    }

    private Element createBusBlock(
        Document doc,
        BusLegendInfo bus,
        String id,
        StyleProvider styleProvider,
        VoltageLevelGraph graph
    ) {
        Element block = doc.createElementNS(XHTML_NS, "div");
        block.setAttribute("class", "legend-block");
        block.setAttribute("id", id);

        Element title = createTitle(doc, bus.busId(), styleProvider.getBusStyles(bus.busId(), graph));
        block.appendChild(title);

        Element table = doc.createElementNS(XHTML_NS, "table");
        table.setAttribute("class", "legend-table");

        for (BusLegendInfo.Caption caption : bus.captions()) {
            Element row = createCaptionRow(doc, id, caption, styleProvider);
            table.appendChild(row);
        }

        block.appendChild(table);
        return block;
    }

    private Element createTitle(Document doc, String text, List<String> circleClasses) {
        Element title = doc.createElementNS(XHTML_NS, "div");
        title.setAttribute("class", "legend-title");

        if (circleClasses != null) {
            Element circle = doc.createElementNS(XHTML_NS, "div");
            writeStyleClasses(circle, circleClasses, "bus-circle");
            circle.appendChild(doc.createTextNode(" ")); // empty div are illegal and causes bugs once displayed
            title.appendChild(circle);
        }

        Element name = doc.createElementNS(XHTML_NS, "span");
        name.setTextContent(text);

        title.appendChild(name);

        return title;
    }

    private Element createCaptionRow(
        Document doc,
        String id,
        BusLegendInfo.Caption caption,
        StyleProvider styleProvider
    ) {
        Element tr = doc.createElementNS(XHTML_NS, "tr");

        writeStyleClasses(tr, styleProvider.getBusLegendCaptionStyles(caption), BUS_LEGEND_INFO);

        tr.setAttribute("id", IdUtil.escapeId(id + "_" + caption.type()));

        Element key = doc.createElementNS(XHTML_NS, "td");
        key.setAttribute("class", "label-cell");
        key.setTextContent(getCaptionTypeColumnLabel(caption));

        Element val = doc.createElementNS(XHTML_NS, "td");
        val.setAttribute("class", "value-cell");
        val.setTextContent(caption.label());

        tr.appendChild(key);
        tr.appendChild(val);
        return tr;
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
}
