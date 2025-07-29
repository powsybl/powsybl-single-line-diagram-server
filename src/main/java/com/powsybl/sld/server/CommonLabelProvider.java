package com.powsybl.sld.server;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.model.graphs.VoltageLevelGraph;
import com.powsybl.sld.svg.BusLegendInfo;
import com.powsybl.sld.svg.DefaultLabelProvider;
import com.powsybl.sld.svg.SvgParameters;

import java.util.List;
import java.util.stream.Collectors;

public class CommonLabelProvider extends DefaultLabelProvider {
    public CommonLabelProvider(Network network, SldComponentLibrary componentLibrary, LayoutParameters layoutParameters, SvgParameters svgParameters) {
        super(network, componentLibrary, layoutParameters, svgParameters);
    }

    @Override
    public List<BusLegendInfo> getBusLegendInfos(VoltageLevelGraph graph) {
        VoltageLevel vl = network.getVoltageLevel(graph.getVoltageLevelInfos().getId());
        return vl.getBusView().getBusStream()
            .map(b -> new BusLegendInfo(b.getId(), List.of(
                new BusLegendInfo.Caption(b.getId(), "busId"),
                new BusLegendInfo.Caption(valueFormatter.formatVoltage(b.getV(), "kV"), "v"),
                new BusLegendInfo.Caption(valueFormatter.formatAngleInDegrees(b.getAngle()), "angle")
            )))
            .collect(Collectors.toList());
    }
}
