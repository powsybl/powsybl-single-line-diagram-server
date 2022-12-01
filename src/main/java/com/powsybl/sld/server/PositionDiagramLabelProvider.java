/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.model.coordinate.Direction;
import com.powsybl.sld.model.nodes.BusNode;
import com.powsybl.sld.model.nodes.EquipmentNode;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.model.nodes.Node;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.svg.LabelPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.sld.server.utils.DiagramUtils.getOrderPositions;

/**
 * @author Ben Daamer ahmed<ahmed.bendaamer at rte-france.com>
 */
public class PositionDiagramLabelProvider extends DefaultDiagramLabelProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionDiagramLabelProvider.class);

    private final Network network;
    private final String voltageLevelId;

    public PositionDiagramLabelProvider(Network network, ComponentLibrary componentLibrary, LayoutParameters layoutParameters, String voltageLevelId) {
        super(network, componentLibrary, layoutParameters);
        this.network = network;
        this.voltageLevelId = voltageLevelId;
    }

    @Override
    public List<NodeLabel> getNodeLabels(Node node, Direction direction) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(direction);
        var labelPosition = getLabelPosition(node, direction);
        if (labelPosition != null) {
            return getLabelOrNameOrId(node).map(label -> new NodeLabel(label, labelPosition, null)).stream().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Optional<String> getLabelOrNameOrId(Node node) {
        if (node instanceof EquipmentNode) {
            EquipmentNode eqNode = (EquipmentNode) node;
            String label = node.getLabel().orElse(layoutParameters.isUseName() ? Objects.toString(eqNode.getName(), "") : eqNode.getEquipmentId());
            Identifiable<?> identifiable = network.getIdentifiable(eqNode.getEquipmentId());
            var vl = network.getVoltageLevel(voltageLevelId);
            if (identifiable != null) {
                ConnectablePosition<?> connectablePosition = (ConnectablePosition<?>) identifiable.getExtension(ConnectablePosition.class);
                if (connectablePosition != null) {
                    Integer order = getOrderPositions(connectablePosition, vl, identifiable, false, LOGGER);
                    if (order != null) {
                        label += " pos: " + order;
                    }
                }
            }
            return Optional.of(label);
        } else {
            return node.getLabel();
        }
    }

    private LabelPosition getLabelPosition(Node node, Direction direction) {
        if (node instanceof FeederNode) {
            return getFeederLabelPosition(node, direction);
        } else if (node instanceof BusNode) {
            return getBusLabelPosition();
        }
        return null;
    }
}
