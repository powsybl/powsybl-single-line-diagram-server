/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.commons.PowsyblException;
import com.powsybl.sld.server.dto.SubstationGeoData;
import com.powsybl.sld.server.dto.nad.ElementParametersInfos;

import java.util.List;

/**
 * @author Maissa SOUISSI<maissa.souissi at rte-france.com>
 */

public final class ResourceUtils {
    private ResourceUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static List<SubstationGeoData> fromStringToSubstationGeoData(String jsonResponse, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new PowsyblException("Failed to parse JSON response", e);
        }
    }

    public static ElementParametersInfos fromStringToElementParametersInfos(String jsonResponse, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new PowsyblException("Failed to parse JSON response", e);
        }
    }

}
