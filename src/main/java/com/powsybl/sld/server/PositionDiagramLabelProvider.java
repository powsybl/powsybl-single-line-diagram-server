/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.model.coordinate.Direction;
import com.powsybl.sld.model.nodes.BusNode;
import com.powsybl.sld.model.nodes.EquipmentNode;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.model.nodes.Node;
import com.powsybl.sld.svg.LabelPosition;
import com.powsybl.sld.svg.LabelProviderFactory;
import com.powsybl.sld.svg.SvgParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Ben Daamer ahmed<ahmed.bendaamer at rte-france.com>
 */
public class PositionDiagramLabelProvider extends CommonLabelProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PositionDiagramLabelProvider.class);

    private final Network network;
    private final String voltageLevelId;

    public PositionDiagramLabelProvider(Network network, SldComponentLibrary componentLibrary, LayoutParameters layoutParameters, SvgParameters svgParameters, String voltageLevelId) {
        super(network, componentLibrary, layoutParameters, svgParameters);
        this.network = network;
        this.voltageLevelId = voltageLevelId;
    }

    public static LabelProviderFactory newLabelProviderFactory(String voltageLevelId) {
        return (network, compLibrary, layoutParameters, svgParameters) -> new PositionDiagramLabelProvider(network, compLibrary, layoutParameters, svgParameters, voltageLevelId);
    }

    @Override
    public List<NodeLabel> getNodeLabels(Node node, Direction direction) {
        Objects.requireNonNull(node);
        Objects.requireNonNull(direction);
        var labelPosition = getNodeLabelPosition(node, direction);
        if (labelPosition != null) {
            return getLabelOrNameOrId(node).map(label -> new NodeLabel(label, labelPosition, null)).stream().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Optional<String> getLabelOrNameOrId(Node node) {
        if (node instanceof EquipmentNode) {
            EquipmentNode eqNode = (EquipmentNode) node;
            String label = node.getLabel().orElse(svgParameters.isUseName() ? Objects.toString(eqNode.getName(), "") : eqNode.getEquipmentId());
            Identifiable<?> identifiable = network.getIdentifiable(eqNode.getEquipmentId());
            var vl = network.getVoltageLevel(voltageLevelId);
            if (identifiable != null) {
                ConnectablePosition<?> connectablePosition = (ConnectablePosition<?>) identifiable.getExtension(ConnectablePosition.class);
                if (connectablePosition != null) {
                    Integer order = getOrderPositions(connectablePosition, vl, identifiable, false);
                    if (order != null) {
                        label += " (pos: " + order + ")";
                    }
                }
            }
            return Optional.of(label);
        } else {
            return node.getLabel();
        }
    }

    private LabelPosition getNodeLabelPosition(Node node, Direction direction) {
        if (node instanceof FeederNode) {
            return getLabelPosition(node, direction);
        } else if (node instanceof BusNode) {
            return getBusLabelPosition();
        }
        return null;
    }

    static Integer getInjectionOrder(ConnectablePosition<?> position) {
        return position.getFeeder().getOrder().orElse(null);
    }

    static Integer getBranchOrder(ConnectablePosition<?> position, VoltageLevel voltageLevel, Branch<?> branch, boolean throwException) {
        Optional<ConnectablePosition.Feeder> feeder = Optional.empty();
        if (branch.getTerminal1().getVoltageLevel().getId().equals(voltageLevel.getId())) {
            feeder = Optional.ofNullable(position.getFeeder1());
        } else if (branch.getTerminal2().getVoltageLevel().getId().equals(voltageLevel.getId())) {
            feeder = Optional.ofNullable(position.getFeeder2());
        } else {
            LOGGER.error("Given voltageLevel {} not found in terminal 1 and terminal 2 of branch", voltageLevel.getId());
            if (throwException) {
                throw new PowsyblException(String.format("Given voltageLevel %s not found in terminal 1 and terminal 2 of branch", voltageLevel.getId()));
            }
        }
        return feeder.flatMap(ConnectablePosition.Feeder::getOrder).orElse(null);
    }

    static Integer get3wtOrder(ConnectablePosition<?> position, VoltageLevel voltageLevel, ThreeWindingsTransformer twt, boolean throwException) {
        Optional<ConnectablePosition.Feeder> feeder = Optional.empty();
        if (twt.getLeg1().getTerminal().getVoltageLevel().getId().equals(voltageLevel.getId())) {
            feeder = Optional.ofNullable(position.getFeeder1());
        } else if (twt.getLeg2().getTerminal().getVoltageLevel().getId().equals(voltageLevel.getId())) {
            feeder = Optional.ofNullable(position.getFeeder2());
        } else if (twt.getLeg3().getTerminal().getVoltageLevel().getId().equals(voltageLevel.getId())) {
            feeder = Optional.ofNullable(position.getFeeder3());
        } else {
            LOGGER.error("Given voltageLevel {} not found in leg 1, leg 2 and leg 3 of ThreeWindingsTransformer", voltageLevel.getId());
            if (throwException) {
                throw new PowsyblException(String.format("Given voltageLevel %s not found in leg 1, leg 2 and leg 3 of ThreeWindingsTransformer", voltageLevel.getId()));
            }
        }
        return feeder.flatMap(ConnectablePosition.Feeder::getOrder).orElse(null);
    }

    static Integer getOrderPositions(ConnectablePosition<?> position, VoltageLevel voltageLevel, Identifiable<?> identifiable, boolean throwException) {
        if (identifiable instanceof Injection) {
            return getInjectionOrder(position);
        } else if (identifiable instanceof Branch) {
            return getBranchOrder(position, voltageLevel, (Branch<?>) identifiable, throwException);
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
}
