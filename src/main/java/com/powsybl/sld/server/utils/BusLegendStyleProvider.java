package com.powsybl.sld.server.utils;

import com.powsybl.sld.svg.BusLegendInfo;
import com.powsybl.sld.svg.styles.EmptyStyleProvider;
import com.powsybl.sld.svg.styles.StyleClassConstants;

import java.util.Collections;
import java.util.List;

public class BusLegendStyleProvider extends EmptyStyleProvider {
    @Override
    public List<String> getBusLegendCaptionStyles(BusLegendInfo.Caption caption) {
        return switch (caption.type()) {
            case "v" -> List.of(StyleClassConstants.VOLTAGE);
            case "angle" -> List.of(StyleClassConstants.ANGLE);
            case "loadSum" -> List.of(StyleClassConstants.STYLE_PREFIX + "loadSum");
            case "generatorSum" -> List.of(StyleClassConstants.STYLE_PREFIX + "generatorSum");
            default -> Collections.emptyList();
        };
    }
}
