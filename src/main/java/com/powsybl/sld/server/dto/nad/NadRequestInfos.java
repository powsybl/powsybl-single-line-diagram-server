package com.powsybl.sld.server.dto.nad;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class NadRequestInfos {
    private UUID nadConfigUuid;
    private UUID filterUuid;
    @Builder.Default
    private List<String> voltageLevelIds = new ArrayList<>();
    @Builder.Default
    private List<String> voltageLevelToExpandIds = new ArrayList<>();
    @Builder.Default
    private List<String> voltageLevelToOmitIds = new ArrayList<>();
    @Builder.Default
    private List<NadVoltageLevelPositionInfos> positions = new ArrayList<>();
    @Builder.Default
    private Boolean withGeoData = true;
}
