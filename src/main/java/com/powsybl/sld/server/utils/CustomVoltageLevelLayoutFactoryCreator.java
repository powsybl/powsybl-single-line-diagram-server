package com.powsybl.sld.server.utils;

import com.powsybl.sld.layout.*;

//TODO: to remove once SmartVoltageLevelLayoutFactory allow us to pass PositionVoltageLevelLayoutFactoryParameters to VoltageLevelLayoutFactory
public interface CustomVoltageLevelLayoutFactoryCreator extends VoltageLevelLayoutFactoryCreator {
    static VoltageLevelLayoutFactoryCreator newCustomVoltageLevelLayoutFactoryCreator() {
        return CustomVoltageLevelLayoutFactory::new;
    }
}
