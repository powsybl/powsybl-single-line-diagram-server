package com.powsybl.sld.server;

import com.powsybl.sld.svg.FeederInfo;
import com.powsybl.sld.svg.styles.EmptyStyleProvider;

import java.util.Collections;
import java.util.List;

public class MeasurementValidityStyleProvider extends EmptyStyleProvider {

    @Override
    public List<String> getCssFilenames() {
        return Collections.singletonList("measurement-validity.css");
    }

    @Override
    public List<String> getFeederInfoStyles(FeederInfo info) {
        return info.getUserDefinedId() == null ? Collections.emptyList() : Collections.singletonList(info.getUserDefinedId());
    }
}
