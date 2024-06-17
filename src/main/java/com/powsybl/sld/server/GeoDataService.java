/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.sld.server;

/**
 * @author Maissa SOUISSI <maissa.souissi at rte-france.com>
 */

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@Service
public class GeoDataService {

    public static final String QUERY_PARAM_SUBSTATION_ID = "substationId";
    public static final String GEO_DATA_API_VERSION = "v1";
    public static final String QUERY_PARAM_VARIANT_ID = "variantId";
    public static final String NETWORK_UUID = "networkUuid";
    static final String SUBSTATIONS = "substations";
    private static final String DELIMITER = "/";
    private final RestTemplate restTemplate;
    @Setter
    private String geoDataServerBaseUri;

    public GeoDataService(@Value("${gridsuite.services.geo-data-server.base-uri:http://geo-data-server/}") String geoDataServerBaseUri,
                          RestTemplate restTemplate) {
        this.geoDataServerBaseUri = geoDataServerBaseUri;
        this.restTemplate = restTemplate;
    }

    private String getGeoDataServerURI() {
        return this.geoDataServerBaseUri + DELIMITER + GEO_DATA_API_VERSION + DELIMITER;
    }

    public String getSubstationsGraphics(UUID networkUuid, String variantId, List<String> substationsIds) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(getGeoDataServerURI() + SUBSTATIONS)
                .queryParam(NETWORK_UUID, networkUuid);

        if (!StringUtils.isBlank(variantId)) {
            uriComponentsBuilder.queryParam(QUERY_PARAM_VARIANT_ID, variantId);
        }

        if (substationsIds != null) {
            uriComponentsBuilder.queryParam(QUERY_PARAM_SUBSTATION_ID, substationsIds);
        }

        var path = uriComponentsBuilder
                .buildAndExpand()
                .toUriString();

        return restTemplate.getForObject(path, String.class);
    }
}

