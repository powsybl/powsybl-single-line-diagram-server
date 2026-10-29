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
import com.powsybl.sld.model.nodes.FeederType;
import com.powsybl.sld.model.nodes.NodeSide;
import com.powsybl.sld.model.nodes.feeders.FeederTwLeg;
import com.powsybl.sld.model.nodes.feeders.FeederWithSides;
import com.powsybl.sld.server.CommonLabelProvider;
import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.SvgParameters;

import java.util.*;
import java.util.function.DoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Kamil MARUT {@literal <kamil.marut at rte-france.com>}
 */
public class MeasurementLabelProvider extends CommonLabelProvider {

    /**
     * Feeder types for which the base feeder infos are filtered out before being displayed
     */
    private static final Set<FeederType> FEEDER_TYPES_WITH_FILTERED_BASED_INFOS = Set.of(FeederType.BRANCH, FeederType.TWO_WINDINGS_TRANSFORMER_LEG, FeederType.THREE_WINDINGS_TRANSFORMER_LEG);

    /**
     * Base feeder infos removed for the feeder types defined in {@link #FEEDER_TYPES_WITH_FILTERED_BASED_INFOS}
     */
    private static final Set<String> FILTERED_BASE_INFO_COMPONENT_TYPES = Set.of(SldComponentTypeName.VALUE_CURRENT, SldComponentTypeName.VALUE_PERMANENT_LIMIT_PERCENTAGE);

    public MeasurementLabelProvider(Network network, SldComponentLibrary componentLibrary, LayoutParameters layoutParameters, SvgParameters svgParameters) {
        super(network, componentLibrary, layoutParameters, svgParameters);
    }

    @Override
    public List<FeederInfo> getFeederInfos(FeederNode node) {
        List<FeederInfo> baseFeederInfos = getFilteredBaseFeederInfos(node);

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

    private List<FeederInfo> getFilteredBaseFeederInfos(FeederNode node) {
        List<FeederInfo> baseFeederInfos = super.getFeederInfos(node);
        if (!FEEDER_TYPES_WITH_FILTERED_BASED_INFOS.contains(node.getFeeder().getFeederType())) {
            return baseFeederInfos;
        }
        return baseFeederInfos.stream()
                .filter(feederInfo -> !FILTERED_BASE_INFO_COMPONENT_TYPES.contains(feederInfo.getComponentType()))
                .toList();
    }

    private List<FeederInfo> getInjectionMeasurementsFeederInfos(FeederNode node) {
        Injection<?> injection = (Injection<?>) network.getIdentifiable(node.getEquipmentId());
        if (injection == null) {
            return Collections.emptyList();
        }
        Measurements<?> measurements = (Measurements<?>) injection.getExtension(Measurements.class);
        InjectionObservability<?> injectionObservability = (InjectionObservability<?>) injection.getExtension(InjectionObservability.class);
        return buildMeasurementsFeederInfos(getMeasurements(measurements), injectionObservability, getExpectedMeasurementTypes(injection));
    }

    private List<FeederInfo> getBranchMeasurementsFeederInfo(FeederNode node, NodeSide nodeSide) {
        Branch<?> branch = (Branch<?>) network.getBranch(node.getEquipmentId());
        if (branch == null) {
            return Collections.emptyList();
        }
        ThreeSides branchSide = ThreeSides.valueOf(nodeSide.name());
        Measurements<?> measurements = (Measurements<?>) branch.getExtension(Measurements.class);
        BranchObservability<?> branchObservability = (BranchObservability<?>) branch.getExtension(BranchObservability.class);
        List<Measurement> sidedMeasurements = getMeasurements(measurements).stream()
                .filter(measurement -> branchSide.equals(measurement.getSide()))
                .toList();
        return buildMeasurementsFeederInfos(sidedMeasurements, branchObservability, getExpectedMeasurementTypes(branch));
    }

    private List<FeederInfo> getHvdcMeasurementsFeederInfos(FeederNode node, NodeSide side) {
        HvdcLine hvdcLine = network.getHvdcLine(node.getEquipmentId());
        if (hvdcLine == null) {
            return Collections.emptyList();
        }
        HvdcConverterStation<?> hvdcConverterStation = NodeSide.ONE.equals(side) ? hvdcLine.getConverterStation1() : hvdcLine.getConverterStation2();
        Measurements<?> measurements = (Measurements<?>) hvdcConverterStation.getExtension(Measurements.class);
        InjectionObservability<?> injectionObservability = (InjectionObservability<?>) hvdcConverterStation.getExtension(InjectionObservability.class);
        return buildMeasurementsFeederInfos(getMeasurements(measurements), injectionObservability, getExpectedMeasurementTypes(hvdcConverterStation));
    }

    private Collection<Measurement> getMeasurements(Measurements<?> measurements) {
        return measurements == null ? List.of() : measurements.getMeasurements();
    }

    private List<Measurement.Type> getExpectedMeasurementTypes(Identifiable<?> equipment) {
        return switch (equipment) {
            case Generator _ -> List.of(Measurement.Type.VOLTAGE, Measurement.Type.REACTIVE_POWER, Measurement.Type.ACTIVE_POWER);
            case Battery _ -> List.of(Measurement.Type.VOLTAGE, Measurement.Type.ACTIVE_POWER);
            case ShuntCompensator _ -> List.of(Measurement.Type.VOLTAGE, Measurement.Type.REACTIVE_POWER);
            case VscConverterStation _ -> List.of(Measurement.Type.VOLTAGE, Measurement.Type.REACTIVE_POWER, Measurement.Type.ACTIVE_POWER);
            case StaticVarCompensator _ -> List.of(Measurement.Type.VOLTAGE, Measurement.Type.REACTIVE_POWER);
            case Load _ -> List.of(Measurement.Type.VOLTAGE, Measurement.Type.REACTIVE_POWER, Measurement.Type.ACTIVE_POWER);
            case TwoWindingsTransformer _ -> List.of(Measurement.Type.VOLTAGE, Measurement.Type.REACTIVE_POWER, Measurement.Type.ACTIVE_POWER);
            case Line _ -> List.of(Measurement.Type.VOLTAGE, Measurement.Type.REACTIVE_POWER, Measurement.Type.ACTIVE_POWER);
            default -> List.of();
        };
    }

    private List<FeederInfo> buildMeasurementsFeederInfos(Collection<Measurement> measurements, Observability<?> observability, List<Measurement.Type> expectedTypes) {
        Set<Measurement.Type> presentTypes = measurements.stream().map(Measurement::getType).collect(Collectors.toSet());
        Stream<TypedFeederInfo> presentInfos = measurements.stream()
                .map(measurement -> new TypedFeederInfo(measurement.getType(), buildMeasurementFeederInfo(measurement, observability)));
        Stream<TypedFeederInfo> missingInfos = expectedTypes.stream()
                .filter(type -> !presentTypes.contains(type))
                .map(type -> new TypedFeederInfo(type, buildMissingMeasurementFeederInfo(type)));
        return Stream.concat(presentInfos, missingInfos)
                .sorted(Comparator.comparingInt((TypedFeederInfo info) -> getMeasurementTypeOrder(info.type()))
                        .thenComparing(TypedFeederInfo::type))
                .map(TypedFeederInfo::feederInfo)
                .toList();
    }

    private static int getMeasurementTypeOrder(Measurement.Type type) {
        return switch (type) {
            case ACTIVE_POWER -> 0;
            case REACTIVE_POWER -> 1;
            case VOLTAGE -> 2;
            default -> 3;
        };
    }

    private record TypedFeederInfo(Measurement.Type type, FeederInfo feederInfo) {
    }

    private FeederInfo buildMeasurementFeederInfo(Measurement measurement, Observability<?> observability) {
        double measurementValue = measurement.getValue();
        boolean measurementValidity = measurement.isValid();
        boolean measurementCriticality = !getMeasurementRedundancy(measurement, observability).orElse(true);

        return new MeasurementFeederInfo(SldComponentTypeName.VALUE_CURRENT, LabelDirection.NONE, measurementValue,
                getMeasurementFormatter(measurement.getType()), measurementValidity, measurementCriticality);
    }

    private FeederInfo buildMissingMeasurementFeederInfo(Measurement.Type type) {
        return new MeasurementFeederInfo(SldComponentTypeName.VALUE_CURRENT, LabelDirection.NONE, Double.NaN,
                getMeasurementFormatter(type), false, false, true);
    }

    private DoubleFunction<String> getMeasurementFormatter(Measurement.Type type) {
        return switch (type) {
            case ACTIVE_POWER -> value -> valueFormatter.formatPower(value, svgParameters.getActivePowerUnit());
            case REACTIVE_POWER -> value -> valueFormatter.formatPower(value, svgParameters.getReactivePowerUnit());
            case VOLTAGE -> valueFormatter::formatVoltage;
            default -> valueFormatter::formatPower;
        };
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
}
