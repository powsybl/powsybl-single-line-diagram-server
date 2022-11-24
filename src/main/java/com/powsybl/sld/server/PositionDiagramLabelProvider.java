package com.powsybl.sld.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.modification.topology.TopologyModificationUtils;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.model.coordinate.Direction;
import com.powsybl.sld.model.coordinate.Side;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.model.nodes.BusNode;
import com.powsybl.sld.model.nodes.EquipmentNode;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.model.nodes.Node;
import com.powsybl.sld.svg.BusInfo;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.svg.ElectricalNodeInfo;
import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.LabelPosition;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class PositionDiagramLabelProvider extends DefaultDiagramLabelProvider {

    private final Network network;
    private String voltageLevelId;

    public PositionDiagramLabelProvider(Network network, ComponentLibrary componentLibrary, LayoutParameters layoutParameters, String voltageLevelId) {
        super(network, componentLibrary, layoutParameters);
        this.network = network;
        this.voltageLevelId = voltageLevelId;
    }

    @Override
    public List<FeederInfo> getFeederInfos(FeederNode node) {
        return Collections.emptyList();
    }

    private Optional<String> getLabelOrNameOrId(Node node, Map<String, List<Integer>> takenFeederPositions) {
        if (node instanceof EquipmentNode) {
            var eqNode = (EquipmentNode) node;
            String label = node.getLabel().orElse(layoutParameters.isUseName() ? Objects.toString(eqNode.getName(), "") : eqNode.getEquipmentId());
            if (MapUtils.isEmpty(takenFeederPositions)) {
                return Optional.of(label);
            }
            var identifiable = network.getIdentifiable(eqNode.getEquipmentId());
            var key = takenFeederPositions.keySet().stream().filter(m -> StringUtils.contains(m.trim(), identifiable.getId().trim())).findFirst().orElse(label);
            return Optional.of(takenFeederPositions.get(key) != null ? label + " pos" + takenFeederPositions.get(key) : label);
        } else {
            return node.getLabel();
        }
    }

    private Map<String, List<Integer>> getFeederPositions() {
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        if (voltageLevel == null) {
            throw new PowsyblException("Voltage level " + voltageLevelId + " not found");
        }

        return TopologyModificationUtils.getFeederPositionsByConnectable(voltageLevel);
    }

    private LabelPosition getLabelPosition(Node node, Direction direction) {
        if (node instanceof FeederNode) {
            return getFeederLabelPosition(node, direction);
        } else if (node instanceof BusNode) {
            return getBusLabelPosition();
        }
        return null;
    }

    @Override
    public List<NodeLabel> getNodeLabels(Node node, Direction direction) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(direction);
        LabelPosition labelPosition = getLabelPosition(node, direction);
        if (labelPosition != null) {
            var takenFeederPositions = getFeederPositions();
            return getLabelOrNameOrId(node, takenFeederPositions).map(label -> new NodeLabel(label, labelPosition, null)).stream().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public String getTooltip(Node node) {
        return null;
    }

    @Override
    public List<NodeDecorator> getNodeDecorators(Node node, Direction direction) {
        return Collections.emptyList();
    }

    @Override
    public List<ElectricalNodeInfo> getElectricalNodesInfos(VoltageLevelGraph graph) {
        return Collections.emptyList();
    }

    @Override
    public Optional<BusInfo> getBusInfo(BusNode node) {
        return Optional.empty();
    }

    @Override
    public Map<String, Side> getBusInfoSides(VoltageLevelGraph graph) {
        Map<String, Side> result = new HashMap<>();
        graph.getNodeBuses().forEach(busNode -> getBusInfo(busNode).ifPresent(busInfo -> result.put(busNode.getId(), busInfo.getAnchor())));
        return Collections.emptyMap();
    }

}
