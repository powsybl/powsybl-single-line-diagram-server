/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
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
                    Integer order = getOrderPositions(connectablePosition, vl, identifiable, false);
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

    private Integer getInjectionOrder(ConnectablePosition<?> position, VoltageLevel voltageLevel, Injection<?> injection, boolean throwException) {
        Integer singleOrder = position.getFeeder().getOrder().orElse(null);
        checkConnectableInVoltageLevel(singleOrder, voltageLevel, injection, throwException);
        return singleOrder;
    }

    private Integer getBranchOrders(ConnectablePosition<?> position, VoltageLevel voltageLevel, Branch<?> branch, boolean throwException) {
        Integer order;
        if (branch.getTerminal1().getVoltageLevel() == voltageLevel) {
            order = position.getFeeder1().getOrder().orElse(null);
        } else if (branch.getTerminal2().getVoltageLevel() == voltageLevel) {
            order = position.getFeeder2().getOrder().orElse(null);
        } else {
            throw new PowsyblException(String.format("Given voltageLevel %s not found in terminal 1 or terminal 2 of branch", voltageLevel.getId()));
        }
        checkConnectableInVoltageLevel(order, voltageLevel, branch, throwException);
        return order;
    }

    private Integer get3wtOrder(ConnectablePosition<?> position, VoltageLevel voltageLevel, ThreeWindingsTransformer twt, boolean throwException) {
        Integer order;
        if (twt.getLeg1().getTerminal().getVoltageLevel() == voltageLevel) {
            order = position.getFeeder1().getOrder().orElse(null);
        } else if (twt.getLeg2().getTerminal().getVoltageLevel() == voltageLevel) {
            order = position.getFeeder2().getOrder().orElse(null);
        } else if (twt.getLeg3().getTerminal().getVoltageLevel() == voltageLevel) {
            order = position.getFeeder3().getOrder().orElse(null);
        } else {
            throw new PowsyblException(String.format("Given voltageLevel %s not found in leg 1, leg 2 or leg 3 of ThreeWindingsTransformer", voltageLevel.getId()));
        }
        checkConnectableInVoltageLevel(order, voltageLevel, twt, throwException);
        return order;
    }

    private Integer getOrderPositions(ConnectablePosition<?> position, VoltageLevel voltageLevel, Identifiable<?> identifiable, boolean throwException) {
        if (identifiable instanceof Injection) {
            return getInjectionOrder(position, voltageLevel, (Injection<?>) identifiable, throwException);
        } else if (identifiable instanceof Branch) {
            return getBranchOrders(position, voltageLevel, (Branch<?>) identifiable, throwException);
        } else if (identifiable instanceof ThreeWindingsTransformer) {
            return get3wtOrder(position, voltageLevel, (ThreeWindingsTransformer) identifiable, throwException);
        } else {
            LOGGER.error("Given connectable not supported: {}", identifiable.getClass().getName());
            if (throwException) {
                throw new PowsyblException(String.format("Given connectable %s not supported: ", identifiable.getClass().getName()));
            }
        }
        return null;
    }

    private void checkConnectableInVoltageLevel(Integer order, VoltageLevel voltageLevel, Connectable<?> connectable, boolean throwException) {
        if (order == null) {
            LOGGER.error("Given connectable {} not found in voltageLevel {}", connectable.getId(), voltageLevel.getId());
            if (throwException) {
                throw new PowsyblException(String.format("Given connectable %s not found in voltageLevel %s ", connectable.getId(), voltageLevel.getId()));
            }
        }
    }
}
