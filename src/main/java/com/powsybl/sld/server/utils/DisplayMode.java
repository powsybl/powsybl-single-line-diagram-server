package com.powsybl.sld.server.utils;

public enum DisplayMode {
    FEEDER_POSITION("FEEDER_POSITION"),
    DEFAULT("DEFAULT");
    private final String value;

    DisplayMode(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
