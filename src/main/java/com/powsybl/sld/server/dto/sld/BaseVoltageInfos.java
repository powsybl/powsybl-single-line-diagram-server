package com.powsybl.sld.server.dto.sld;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class BaseVoltageInfos {
    String name;
    Integer minValue;
    Integer maxValue;
    String profile;
}
