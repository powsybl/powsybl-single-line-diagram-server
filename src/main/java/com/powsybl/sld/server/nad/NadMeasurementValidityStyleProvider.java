/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.nad;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.config.BaseVoltagesConfig;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.extensions.Measurement;
import com.powsybl.iidm.network.extensions.Measurements;
import com.powsybl.nad.model.BranchEdge;
import com.powsybl.nad.model.BusNode;
import com.powsybl.nad.model.ThreeWtEdge;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Reuses the SLD "measurement validity" idea for the NAD: colors branches, three-winding transformers and
 * the electrical nodes (buses) they connect to according to their Ptm measurement (green when below the
 * threshold, red when above), and tags the TM voltage displayed per electrical node with its validity
 * (see {@link NadMeasurementValidityLabelProvider}).
 */
public class NadMeasurementValidityStyleProvider extends TopologicalStyleProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NadMeasurementValidityStyleProvider.class);

    public static final String LOW_PTM_BRANCH_CLASS = "nad-low-ptm-branch";
    public static final String HIGH_PTM_BRANCH_CLASS = "nad-high-ptm-branch";
    public static final String LOW_PTM_BUS_CLASS = "nad-low-ptm-bus";
    public static final String HIGH_PTM_BUS_CLASS = "nad-high-ptm-bus";
    public static final String MEASUREMENT_VALID_CLASS = "nad-measurement-valid";
    public static final String MEASUREMENT_INVALID_CLASS = "nad-measurement-invalid";
    public static final String VOLTAGE_VALID_CLASS = "nad-voltage-valid";
    public static final String VOLTAGE_INVALID_CLASS = "nad-voltage-invalid";
    public static final String INVALID_VOLTAGE_BRANCH_CLASS = "nad-invalid-voltage-branch";

    private static final double PTM_THRESHOLD = 18.0;

    private final Set<String> lowPtmIds;
    private final Set<String> highPtmIds;

    public NadMeasurementValidityStyleProvider(Network network, BaseVoltagesConfig baseVoltageStyle) {
        super(network, baseVoltageStyle);
        this.lowPtmIds = new HashSet<>();
        this.highPtmIds = new HashSet<>();
        classifyByPtm(network.getLines());
        classifyByPtm(network.getTwoWindingsTransformers());
        classifyByPtm(network.getThreeWindingsTransformers());
    }

    private void classifyByPtm(Iterable<? extends Connectable<?>> equipments) {
        for (Connectable<?> equipment : equipments) {
            Measurements<?> measurements = (Measurements<?>) equipment.getExtension(Measurements.class);
            if (measurements == null) {
                continue;
            }
            measurements.getMeasurements(Measurement.Type.ACTIVE_POWER).stream().findFirst().ifPresent(m -> {
                if (Math.abs(m.getValue()) > PTM_THRESHOLD) {
                    highPtmIds.add(equipment.getId());
                } else {
                    lowPtmIds.add(equipment.getId());
                }
            });
        }
    }

    @Override
    public List<String> getCssFilenames() {
        List<String> filenames = new ArrayList<>(super.getCssFilenames());
        filenames.add("nad-measurement-validity.css");
        return filenames;
    }

    @Override
    public List<String> getBranchEdgeStyleClasses(BranchEdge branchEdge) {
        List<String> styles = new ArrayList<>(super.getBranchEdgeStyleClasses(branchEdge));
        addPtmClass(styles, branchEdge.getEquipmentId());
        addInvalidVoltageClass(styles, branchEdge.getEquipmentId());
        return styles;
    }

    @Override
    public List<String> getThreeWtEdgeStyleClasses(ThreeWtEdge threeWtEdge) {
        List<String> styles = new ArrayList<>(super.getThreeWtEdgeStyleClasses(threeWtEdge));
        addPtmClass(styles, threeWtEdge.getEquipmentId());
        addInvalidVoltageClass(styles, threeWtEdge.getEquipmentId());
        return styles;
    }

    @Override
    public List<String> getBusNodeStyleClasses(BusNode busNode) {
        List<String> styles = new ArrayList<>(super.getBusNodeStyleClasses(busNode));
        Bus bus = network.getBusView().getBus(busNode.getEquipmentId());
        if (bus != null) {
            try {
                addConnectedPtmClass(styles, bus);
                addVoltageValidityClass(styles, bus);
            } catch (PowsyblException e) {
                LOGGER.warn("Could not resolve Ptm/voltage style for bus '{}'", bus.getId(), e);
            }
        }
        return styles;
    }

    @Override
    public List<String> getEdgeInfoStyleClasses(String infoType) {
        if (NadMeasurementValidityLabelProvider.MEASUREMENT_VALID.equals(infoType)) {
            return Collections.singletonList(MEASUREMENT_VALID_CLASS);
        }
        if (NadMeasurementValidityLabelProvider.MEASUREMENT_INVALID.equals(infoType)) {
            return Collections.singletonList(MEASUREMENT_INVALID_CLASS);
        }
        return super.getEdgeInfoStyleClasses(infoType);
    }

    private void addPtmClass(List<String> styles, String equipmentId) {
        if (highPtmIds.contains(equipmentId)) {
            styles.add(HIGH_PTM_BRANCH_CLASS);
        } else if (lowPtmIds.contains(equipmentId)) {
            styles.add(LOW_PTM_BRANCH_CLASS);
        }
    }

    private void addConnectedPtmClass(List<String> styles, Bus bus) {
        if (isConnectedToAny(bus, highPtmIds)) {
            styles.add(HIGH_PTM_BUS_CLASS);
        } else if (isConnectedToAny(bus, lowPtmIds)) {
            styles.add(LOW_PTM_BUS_CLASS);
        }
    }

    private boolean isConnectedToAny(Bus bus, Set<String> equipmentIds) {
        return bus.getConnectedTerminalStream()
                .map(Terminal::getConnectable)
                .map(Connectable::getId)
                .anyMatch(equipmentIds::contains);
    }

    private void addVoltageValidityClass(List<String> styles, Bus bus) {
        getBusVoltageValidity(bus).ifPresent(valid -> styles.add(valid ? VOLTAGE_VALID_CLASS : VOLTAGE_INVALID_CLASS));
    }

    private void addInvalidVoltageClass(List<String> styles, String equipmentId) {
        try {
            if (isConnectedToInvalidVoltageBus(equipmentId)) {
                styles.add(INVALID_VOLTAGE_BRANCH_CLASS);
            }
        } catch (PowsyblException e) {
            LOGGER.warn("Could not resolve voltage validity for equipment '{}'", equipmentId, e);
        }
    }

    private boolean isConnectedToInvalidVoltageBus(String equipmentId) {
        Connectable<?> connectable = network.getConnectable(equipmentId);
        if (connectable == null) {
            return false;
        }
        return connectable.getTerminals().stream()
                .map(t -> t.getBusView().getBus())
                .filter(Objects::nonNull)
                .anyMatch(bus -> !getBusVoltageValidity(bus).orElse(true));
    }

    private Optional<Boolean> getBusVoltageValidity(Bus bus) {
        return findRepresentativeBusbarSection(bus)
                .map(bbs -> (Measurements<?>) bbs.getExtension(Measurements.class))
                .filter(Objects::nonNull)
                .flatMap(measurements -> measurements.getMeasurements(Measurement.Type.VOLTAGE).stream().findFirst())
                .map(Measurement::isValid);
    }

    private Optional<BusbarSection> findRepresentativeBusbarSection(Bus bus) {
        return bus.getConnectedTerminalStream()
                .map(Terminal::getConnectable)
                .filter(BusbarSection.class::isInstance)
                .map(BusbarSection.class::cast)
                .findFirst();
    }
}
