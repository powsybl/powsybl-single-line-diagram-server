package com.powsybl.sld.server;

import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.extensions.Measurement;
import com.powsybl.iidm.network.extensions.Measurements;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.model.nodes.FeederNode;
import com.powsybl.sld.model.nodes.NodeSide;
import com.powsybl.sld.model.nodes.feeders.FeederWithSides;
import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.LabelProvider;
import com.powsybl.sld.svg.SvgParameters;
import com.powsybl.sld.svg.ValueFeederInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.powsybl.sld.library.SldComponentTypeName.ARROW_ACTIVE;
import static com.powsybl.sld.library.SldComponentTypeName.ARROW_REACTIVE;
import static com.powsybl.sld.library.SldComponentTypeName.VALUE_CURRENT;

public class MeasurementValidityLabelProvider extends CommonLabelProvider {
    public static final String VALID = "sld-measurement-valid";
    public static final String INVALID = "sld-measurement-invalid";

    public MeasurementValidityLabelProvider(Network network, SldComponentLibrary componentLibrary, LayoutParameters layoutParameters, SvgParameters svgParameters) {
        super(network, componentLibrary, layoutParameters, svgParameters);
    }

    @Override
    public List<FeederInfo> getFeederInfos(FeederNode node) {
        List<FeederInfo> baseInfos = super.getFeederInfos(node);
        if (!(node.getFeeder() instanceof FeederWithSides feederWithSides)) {
            return baseInfos;
        }
        Identifiable<?> identifiable = network.getIdentifiable(node.getEquipmentId());
        if (!(identifiable instanceof Line line)) {
            return baseInfos;
        }

        NodeSide side = feederWithSides.getSide();
        Optional<Measurement> pMeasurement = findMeasurement(line, side, Measurement.Type.ACTIVE_POWER);
        Optional<Measurement> qMeasurement = findMeasurement(line, side, Measurement.Type.REACTIVE_POWER);

        List<FeederInfo> updated = new ArrayList<>();
        for (FeederInfo info : baseInfos) {
            updated.add(info);
            if (info instanceof ValueFeederInfo valueFeederInfo) {
                if (ARROW_ACTIVE.equals(valueFeederInfo.getComponentType())) {
                    updated.add(createMeasurementInfo("Ptm", pMeasurement));
                } else if (ARROW_REACTIVE.equals(valueFeederInfo.getComponentType())) {
                    updated.add(createMeasurementInfo("Qtm", qMeasurement));
                }
            }
        }
        return updated;
    }

    private FeederInfo createMeasurementInfo(String label, Optional<Measurement> measurement) {
        String value = measurement.map(m -> String.valueOf(Math.round(Math.abs(m.getValue())))).orElse("-");
        String validity = measurement.map(m -> m.isValid() ? "valid" : "invalid").orElse("unknown");
        String styleClass = measurement.map(m -> m.isValid() ? VALID : INVALID).orElse(null);
        return new TaggedValueFeederInfo(VALUE_CURRENT, LabelProvider.LabelDirection.NONE, label, value + " (" + validity + ")", styleClass);
    }

    // Investigation helper: reads line measurements for active/reactive and both sides through findMeasurement(...)
    private Optional<Measurement> findMeasurement(Line line, NodeSide side, Measurement.Type type) {
        Measurements<?> measurements = (Measurements<?>) line.getExtension(Measurements.class);
        if (measurements == null) {
            return Optional.empty();
        }
        int sideNum = side == NodeSide.ONE ? 1 : 2;
        return measurements.getMeasurements(type).stream()
                .filter(measurement -> measurement.getSide() != null && measurement.getSide().getNum() == sideNum)
                .findFirst();
    }

    private static final class TaggedValueFeederInfo extends ValueFeederInfo {
        private final String userDefinedId;

        private TaggedValueFeederInfo(String componentType, LabelProvider.LabelDirection labelDirection, String leftLabel, String rightLabel, String userDefinedId) {
            super(componentType, labelDirection, leftLabel, rightLabel);
            this.userDefinedId = userDefinedId;
        }

        @Override
        public String getUserDefinedId() {
            return userDefinedId;
        }
    }
}
