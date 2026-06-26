/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.sld.server.estim;

import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.*;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.library.SldComponentTypeName;
import com.powsybl.sld.model.coordinate.Direction;
import com.powsybl.sld.model.nodes.Feeder;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.model.nodes.NodeSide;
import com.powsybl.sld.model.nodes.feeders.FeederTwLeg;
import com.powsybl.sld.model.nodes.feeders.FeederWithSides;
import com.powsybl.sld.server.CommonLabelProvider;
import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.SvgParameters;
import com.powsybl.sld.svg.ValueFeederInfo;

import java.util.*;
import java.util.stream.Stream;

/**
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
public class StateEstimationLabelProvider extends CommonLabelProvider {

    private static final String VALID_MEASUREMENT_CSS = "sld-measurement-valid";
    private static final String INVALID_MEASUREMENT_CSS = "sld-measurement-invalid";
    private static final String CRITICAL_MEASUREMENT_CSS = "sld-measurement-critical";

    public StateEstimationLabelProvider(Network network, SldComponentLibrary componentLibrary, LayoutParameters layoutParameters, SvgParameters svgParameters) {
        super(network, componentLibrary, layoutParameters, svgParameters);
    }

    @Override
    public List<FeederInfo> getFeederInfos(FeederNode node) {
        List<FeederInfo> baseFeederInfos = super.getFeederInfos(node);

        Feeder feeder = node.getFeeder();
        List<FeederInfo> measurementsFeederInfos = new ArrayList<>(switch (feeder.getFeederType()) {
                case INJECTION -> getInjectionMeasurementsFeederInfos(node);
                case BRANCH -> getBranchMeasurementsFeederInfo(node, ((FeederWithSides) feeder).getSide());
                case TWO_WINDINGS_TRANSFORMER_LEG -> getBranchMeasurementsFeederInfo(node, ((FeederTwLeg) feeder).getSide());
                case HVDC -> getHvdcMeasurementsFeederInfos(node, ((FeederWithSides) feeder).getSide());
                default -> Collections.<FeederInfo>emptyList();
            });
        if (node.getDirection() == Direction.BOTTOM && !svgParameters.isFeederInfoSymmetry()) {
            return Stream.concat(measurementsFeederInfos.reversed().stream(), baseFeederInfos.stream()).toList();
        }

        return Stream.concat(baseFeederInfos.stream(), measurementsFeederInfos.stream()).toList();
    }

    private List<FeederInfo> getInjectionMeasurementsFeederInfos(FeederNode node) {
        Injection<?> injection = (Injection<?>) network.getIdentifiable(node.getEquipmentId());
        if (injection == null) {
            return Collections.emptyList();
        }
        Measurements<?> measurements = (Measurements<?>) injection.getExtension(Measurements.class);
        InjectionObservability<?> injectionObservability = (InjectionObservability<?>) injection.getExtension(InjectionObservability.class);
        return buildMeasurementsFeederInfos(measurements, injectionObservability);
    }

    private List<FeederInfo> getBranchMeasurementsFeederInfo(FeederNode node, NodeSide nodeSide) {
        Branch<?> branch = (Branch<?>) network.getBranch(node.getEquipmentId());
        if (branch == null) {
            return Collections.emptyList();
        }
        ThreeSides branchSide = ThreeSides.valueOf(nodeSide.name());
        Measurements<?> measurements = (Measurements<?>) branch.getExtension(Measurements.class);
        BranchObservability<?> branchObservability = (BranchObservability<?>) branch.getExtension(BranchObservability.class);
        return buildSidedMeasurementsFeederInfos(measurements, branchSide, branchObservability);
    }

    private List<FeederInfo> getHvdcMeasurementsFeederInfos(FeederNode node, NodeSide side) {
        HvdcLine hvdcLine = network.getHvdcLine(node.getEquipmentId());
        if (hvdcLine == null) {
            return Collections.emptyList();
        }
        HvdcConverterStation<?> hvdcConverterStation = NodeSide.ONE.equals(side) ? hvdcLine.getConverterStation1() : hvdcLine.getConverterStation2();
        Measurements<?> measurements = (Measurements<?>) hvdcConverterStation.getExtension(Measurements.class);
        InjectionObservability<?> injectionObservability = (InjectionObservability<?>) hvdcConverterStation.getExtension(InjectionObservability.class);
        return buildMeasurementsFeederInfos(measurements, injectionObservability);
    }

    private List<FeederInfo> buildSidedMeasurementsFeederInfos(Measurements<?> measurements, ThreeSides measurementSide, BranchObservability<?> branchObservability) {
        if (measurements == null) {
            return Collections.emptyList();
        }
        return measurements.getMeasurements().stream()
                .filter(measurement -> measurementSide.equals(measurement.getSide()))
                .sorted(Comparator.comparing(Measurement::getType))
                .map(measurement -> buildMeasurementFeederInfo(measurement, branchObservability))
                .toList();
    }

    private List<FeederInfo> buildMeasurementsFeederInfos(Measurements<?> measurements, Observability<?> observability) {
        if (measurements == null) {
            return Collections.emptyList();
        }
        return measurements.getMeasurements().stream()
                .sorted(Comparator.comparing(Measurement::getType))
                .map(measurement -> buildMeasurementFeederInfo(measurement, observability))
                .toList();
    }

    private FeederInfo buildMeasurementFeederInfo(Measurement measurement, Observability<?> observability) {
        Measurement.Type measurementType = measurement.getType();
        Optional<Boolean> measurementRedundancy = getMeasurementRedundancy(measurement, observability);

        double measurementPower = measurement.getValue();
        String measurementPowerUnit = getMeasurementPowerUnit(measurementType);
        String measurementCssClass = getMeasurementCssClass(measurement, measurementRedundancy);

        return new ValueFeederInfo(SldComponentTypeName.VALUE_CURRENT, LabelDirection.NONE, measurementPower, measurementPowerUnit, (value, unit) -> valueFormatter.formatPower(measurementPower, measurementPowerUnit), measurementCssClass);
    }

    private Optional<Boolean> getMeasurementRedundancy(Measurement measurement, Observability<?> observability) {
        if (observability == null) {
            return Optional.empty();
        }

        Measurement.Type measurementType = measurement.getType();
        if (observability instanceof InjectionObservability<?> injectionObservability) {
            return getInjectionMeasurementRedundancy(injectionObservability, measurementType);
        } else if (observability instanceof BranchObservability<?> branchObservability) {
            return getBranchMeasurementRedundancy(measurement, branchObservability, measurementType);
        }
        return Optional.empty();
    }

    private Optional<Boolean> getInjectionMeasurementRedundancy(InjectionObservability<?> injectionObservability, Measurement.Type measurementType) {
        return switch (measurementType) {
            case VOLTAGE -> isMeasurementRedundant(injectionObservability.getQualityV());
            case ACTIVE_POWER -> isMeasurementRedundant(injectionObservability.getQualityP());
            case REACTIVE_POWER -> isMeasurementRedundant(injectionObservability.getQualityQ());
            default -> Optional.empty();
        };
    }

    private Optional<Boolean> getBranchMeasurementRedundancy(Measurement measurement, BranchObservability<?> branchObservability, Measurement.Type measurementType) {
        ThreeSides measurementSide = measurement.getSide();

        return switch (measurementType) {
            case ACTIVE_POWER -> ThreeSides.ONE.equals(measurementSide) ? isMeasurementRedundant(branchObservability.getQualityP1()) : isMeasurementRedundant(branchObservability.getQualityP2());
            case REACTIVE_POWER -> ThreeSides.ONE.equals(measurementSide) ? isMeasurementRedundant(branchObservability.getQualityQ1()) : isMeasurementRedundant(branchObservability.getQualityQ2());
            default -> Optional.empty();
        };
    }

    private Optional<Boolean> isMeasurementRedundant(ObservabilityQuality<?> observabilityQuality) {
        return observabilityQuality != null ? observabilityQuality.isRedundant() : Optional.empty();
    }

    private String getMeasurementPowerUnit(Measurement.Type measurementType) {
        return switch (measurementType) {
            case VOLTAGE -> "kV";
            case ACTIVE_POWER -> "MW";
            case REACTIVE_POWER -> "Mvar";
            default -> svgParameters.getUndefinedValueSymbol();
        };
    }

    private String getMeasurementCssClass(Measurement measurement, Optional<Boolean> measurementRedundancy) {
        String cssClass = measurement.isValid() ? VALID_MEASUREMENT_CSS : INVALID_MEASUREMENT_CSS;
        if (measurementRedundancy.isPresent() && !measurementRedundancy.get()) {
            cssClass += " " + CRITICAL_MEASUREMENT_CSS;
        }
        return cssClass;
    }
}
