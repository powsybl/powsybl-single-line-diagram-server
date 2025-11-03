package com.powsybl.sld.server.dto.sld;

import java.util.List;

import com.powsybl.commons.config.BaseVoltagesConfig;
import com.powsybl.sld.server.dto.CurrentLimitViolationInfos;

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
public class SldRequestInfos {
    private List<CurrentLimitViolationInfos> currentLimitViolationInfos;
    private BaseVoltagesConfig baseVoltagesConfig;
}
