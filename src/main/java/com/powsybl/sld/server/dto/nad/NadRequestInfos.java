package com.powsybl.sld.server.dto.nad;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class NadRequestInfos {
    private UUID nadConfigUuid;
    private UUID filterUuid;
    private List<String> voltageLevelIds;
    private List<String> voltageLevelToExpandIds;
    private List<String> voltageLevelToOmitIds;
    private List<NadVoltageLevelPositionInfos> positions;
    @Builder.Default
    private Boolean withGeoData = true;
}
