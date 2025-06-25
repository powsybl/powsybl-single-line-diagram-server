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
    private List<String> voltageLevelsIds;
    private List<String> voltageLevelsToExpandIds;
    private List<String> voltageLevelsToOmitIds;
    private List<NadVoltageLevelPositionInfos> positions;
}
