package com.powsybl.sld.server.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@NoArgsConstructor
public class SvgGenerationMetadata {
    List<CurrentLimitViolationInfos> currentLimitViolationInfos;
    Map<String, Double> busIdToIccValues;

    public SvgGenerationMetadata(List<CurrentLimitViolationInfos> currentLimitViolationInfos) {
        this.currentLimitViolationInfos = currentLimitViolationInfos;
    }

    public SvgGenerationMetadata(List<CurrentLimitViolationInfos> currentLimitViolationInfos, Map<String, Double> busIdToIccValues) {
        this.currentLimitViolationInfos = currentLimitViolationInfos;
        this.busIdToIccValues = busIdToIccValues;
    }
}
