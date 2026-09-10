/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.nad;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.extensions.Extendable;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.Measurement;
import com.powsybl.iidm.network.extensions.Measurements;
import com.powsybl.nad.model.BranchEdge;
import com.powsybl.nad.model.ThreeWtEdge;
import com.powsybl.nad.svg.EdgeInfo;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.VoltageLevelLegend;
import com.powsybl.sld.server.NadLabelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reuses the SLD "measurement validity" idea for the NAD: replaces the transiting active/reactive power
 * displayed on branches and three-winding transformers with their TM (telemeasurement) values, and the
 * voltage displayed per electrical node with the TM voltage of one of its busbar sections. Every TM value
 * is tagged with a dedicated info type so that {@link NadMeasurementValidityStyleProvider} can color it
 * according to its validity.
 */
public class NadMeasurementValidityLabelProvider extends NadLabelProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(NadMeasurementValidityLabelProvider.class);

    public static final String MEASUREMENT_VALID = "MeasurementValid";
    public static final String MEASUREMENT_INVALID = "MeasurementInvalid";

    public NadMeasurementValidityLabelProvider(Network network, SvgParameters svgParameters) {
        super(network, svgParameters);
    }

    @Override
    public Optional<EdgeInfo> getBranchEdgeInfo(String branchId, String branchType) {
        Optional<EdgeInfo> baseInfo = super.getBranchEdgeInfo(branchId, branchType);
        if (baseInfo.isEmpty()) {
            return baseInfo;
        }
        Branch<?> branch = getNetwork().getBranch(branchId);
        if (branch == null) {
            return baseInfo;
        }
        return Optional.of(withMeasurementInfo(baseInfo.get(), branch));
    }

    @Override
    public Optional<EdgeInfo> getBranchEdgeInfo(String branchId, BranchEdge.Side side, String branchType) {
        // Suppress the per-side calculated flow (e.g. reactive power) shown at each end of the branch:
        // only the TM values from getBranchEdgeInfo(String, String) (the middle info) should be displayed.
        return Optional.empty();
    }

    @Override
    public Optional<EdgeInfo> getThreeWindingTransformerEdgeInfo(String threeWindingTransformerId, ThreeWtEdge.Side side) {
        Optional<EdgeInfo> baseInfo = super.getThreeWindingTransformerEdgeInfo(threeWindingTransformerId, side);
        if (baseInfo.isEmpty()) {
            return baseInfo;
        }
        ThreeWindingsTransformer twt = getNetwork().getThreeWindingsTransformer(threeWindingTransformerId);
        if (twt == null) {
            return baseInfo;
        }
        return Optional.of(withMeasurementInfo(baseInfo.get(), twt));
    }

    @Override
    public VoltageLevelLegend getVoltageLevelLegend(String voltageLevelId) {
        VoltageLevelLegend baseLegend = super.getVoltageLevelLegend(voltageLevelId);
        VoltageLevel voltageLevel = getNetwork().getVoltageLevel(voltageLevelId);
        if (voltageLevel == null) {
            return baseLegend;
        }
        Map<String, String> busLegend = new HashMap<>(baseLegend.busLegend());
        for (Bus bus : voltageLevel.getBusView().getBuses()) {
            try {
                findRepresentativeBusbarSection(bus)
                        .flatMap(bbs -> findMeasurement(bbs, Measurement.Type.VOLTAGE))
                        .ifPresent(m -> busLegend.put(bus.getId(), formatVoltage(m)));
            } catch (PowsyblException e) {
                LOGGER.warn("Could not resolve TM voltage for bus '{}' of voltage level '{}'", bus.getId(), voltageLevelId, e);
            }
        }
        return new VoltageLevelLegend(baseLegend.legendHeader(), baseLegend.legendFooter(), busLegend);
    }

    private EdgeInfo withMeasurementInfo(EdgeInfo baseInfo, Extendable<?> equipment) {
        Optional<Measurement> pMeasurement = findMeasurement(equipment, Measurement.Type.ACTIVE_POWER);
        Optional<Measurement> qMeasurement = findMeasurement(equipment, Measurement.Type.REACTIVE_POWER);
        if (pMeasurement.isEmpty() && qMeasurement.isEmpty()) {
            return baseInfo;
        }

        String infoTypeA = pMeasurement.map(this::infoType).orElse(baseInfo.getInfoTypeA());
        String labelA = pMeasurement.map(this::formatPower).orElse(baseInfo.getLabelA().orElse(null));
        String infoTypeB = qMeasurement.map(this::infoType).orElse(baseInfo.getInfoTypeB());
        String labelB = qMeasurement.map(this::formatPower).orElse(baseInfo.getLabelB().orElse(null));

        return new EdgeInfo(
                infoTypeA,
                infoTypeB,
                baseInfo.getDirectionA().orElse(null),
                baseInfo.getDirectionB().orElse(null),
                labelA,
                labelB,
                baseInfo.getComponentType().orElse(null));
    }

    private String infoType(Measurement measurement) {
        return measurement.isValid() ? MEASUREMENT_VALID : MEASUREMENT_INVALID;
    }

    private String formatPower(Measurement measurement) {
        return getValueFormatter().formatPowerWithAbs(measurement.getValue(), "");
    }

    private String formatVoltage(Measurement measurement) {
        return getValueFormatter().formatVoltage(measurement.getValue(), "kV");
    }

    private Optional<BusbarSection> findRepresentativeBusbarSection(Bus bus) {
        return bus.getConnectedTerminalStream()
                .map(Terminal::getConnectable)
                .filter(BusbarSection.class::isInstance)
                .map(BusbarSection.class::cast)
                .findFirst();
    }

    private Optional<Measurement> findMeasurement(Extendable<?> equipment, Measurement.Type type) {
        Measurements<?> measurements = (Measurements<?>) equipment.getExtension(Measurements.class);
        if (measurements == null) {
            return Optional.empty();
        }
        return measurements.getMeasurements(type).stream().findFirst();
    }
}
