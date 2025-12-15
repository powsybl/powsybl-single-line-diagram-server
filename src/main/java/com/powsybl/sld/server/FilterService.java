/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.sld.server.dto.IdentifiableAttributes;
import lombok.NonNull;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.UUID;

/**
 * @author Charly BOUTIER <charly.boutier at rte-france.com>
 */

@Service
public class FilterService {

    public static final String FILTER_API_VERSION = "v1";
    public static final String QUERY_PARAM_VARIANT_ID = "variantId";
    public static final String QUERY_PARAM_NETWORK_UUID = "networkUuid";
    private static final String DELIMITER = "/";
    private final RestTemplate restTemplate;
    @Setter
    private String filterServerBaseUri;

    public static final String FILTER_END_POINT_EXPORT = "/filters/{id}/export";

    public FilterService(@Value("${gridsuite.services.filter-server.base-uri:http://filter-server/}") String filterServerBaseUri,
                         RestTemplate restTemplate) {
        this.filterServerBaseUri = filterServerBaseUri;
        this.restTemplate = restTemplate;
    }

    private String getFilterServerURI() {
        return this.filterServerBaseUri + DELIMITER + FILTER_API_VERSION + DELIMITER;
    }

    public List<IdentifiableAttributes> exportFilter(@NonNull UUID networkUuid, String variantId, @NonNull UUID filterUuid) {
        String endPointUrl = getFilterServerURI() + FILTER_END_POINT_EXPORT;

        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(endPointUrl);
        uriComponentsBuilder.queryParam(QUERY_PARAM_NETWORK_UUID, networkUuid);
        if (variantId != null && !variantId.isBlank()) {
            uriComponentsBuilder.queryParam(QUERY_PARAM_VARIANT_ID, variantId);
        }
        var uriComponent = uriComponentsBuilder.buildAndExpand(filterUuid);
        return restTemplate.exchange(uriComponent.toUriString(), HttpMethod.GET, null,
            new ParameterizedTypeReference<List<IdentifiableAttributes>>() {
            }).getBody();
    }
}
