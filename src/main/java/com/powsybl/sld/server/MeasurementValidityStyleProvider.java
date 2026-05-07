package com.powsybl.sld.server;

import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.Measurement;
import com.powsybl.iidm.network.extensions.Measurements;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.model.graphs.Graph;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.model.nodes.*;
import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.styles.EmptyStyleProvider;

import java.util.*;

public class MeasurementValidityStyleProvider extends EmptyStyleProvider {

    public static final String HIGH_PTM_LINE_CLASS = "sld-high-ptm-line";
    public static final String HIGH_PTM_BUS_CLASS = "sld-high-ptm-bus";
    private static final double PTM_THRESHOLD = 18.0;

    private final Set<String> highPtmLineIds;
    // lazily computed per graph rendering
    private final Map<Graph, Set<Edge>> highlightedEdgesCache = new WeakHashMap<>();

    public MeasurementValidityStyleProvider(Network network) {
        this.highPtmLineIds = new HashSet<>();
        for (Line line : network.getLines()) {
            Measurements<?> measurements = (Measurements<?>) line.getExtension(Measurements.class);
            if (measurements == null) {
                continue;
            }
            boolean isHighPtm = measurements.getMeasurements(Measurement.Type.ACTIVE_POWER).stream()
                    .anyMatch(m -> Math.abs(m.getValue()) > PTM_THRESHOLD);
            if (isHighPtm) {
                highPtmLineIds.add(line.getId());
            }
        }
    }

    @Override
    public List<String> getCssFilenames() {
        return Collections.singletonList("measurement-validity.css");
    }

    @Override
    public List<String> getFeederInfoStyles(FeederInfo info) {
        return info.getUserDefinedId() == null ? Collections.emptyList() : Collections.singletonList(info.getUserDefinedId());
    }

    @Override
    public List<String> getEdgeStyles(Graph graph, Edge edge) {
        if (getHighlightedEdges(graph).contains(edge)) {
            return List.of(HIGH_PTM_LINE_CLASS);
        }
        return Collections.emptyList();
    }

    @Override
    public List<String> getNodeStyles(VoltageLevelGraph graph, Node node, SldComponentLibrary componentLibrary, boolean showInternalNodes) {
        if (!(node instanceof BusNode)) {
            return Collections.emptyList();
        }
        for (Edge edge : node.getAdjacentEdges()) {
            if (getHighlightedEdges(graph).contains(edge)) {
                return List.of(HIGH_PTM_BUS_CLASS);
            }
        }
        return Collections.emptyList();
    }

    private Set<Edge> getHighlightedEdges(Graph graph) {
        return highlightedEdgesCache.computeIfAbsent(graph, this::computeHighlightedEdges);
    }

    private Set<Edge> computeHighlightedEdges(Graph graph) {
        Set<Edge> highlighted = new HashSet<>();
        graph.getAllNodesStream()
                .filter(n -> n instanceof FeederNode fn && highPtmLineIds.contains(fn.getEquipmentId()))
                .forEach(fn -> bfsHighlight(fn, highlighted));
        return highlighted;
    }

    /**
     * BFS from a FeederNode toward the bus, collecting edges.
     * Stops at open switches (their edge is not colored).
     */
    private void bfsHighlight(Node start, Set<Edge> highlighted) {
        Set<Node> visited = new HashSet<>();
        Queue<Node> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            for (Edge edge : current.getAdjacentEdges()) {
                Node other = otherNode(edge, current);
                if (other == null || visited.contains(other)) {
                    continue;
                }
                if (other instanceof SwitchNode sw && sw.isOpen()) {
                    continue;
                }
                highlighted.add(edge);
                visited.add(other);
                if (!(other instanceof BusNode)) {
                    queue.add(other);
                }
            }
        }
    }

    private static Node otherNode(Edge edge, Node node) {
        List<Node> nodes = edge.getNodes();
        return nodes.size() == 2 ? (nodes.get(0).equals(node) ? nodes.get(1) : nodes.get(0)) : null;
    }
}
