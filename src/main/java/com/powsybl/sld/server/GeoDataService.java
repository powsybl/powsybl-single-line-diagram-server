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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.extensions.LinePosition;
import com.powsybl.iidm.network.extensions.LinePositionAdder;
import com.powsybl.iidm.network.extensions.SubstationPosition;
import com.powsybl.iidm.network.extensions.SubstationPositionAdder;
import com.powsybl.sld.server.dto.Coordinate;
import com.powsybl.sld.server.dto.LineGeoData;
import com.powsybl.sld.server.dto.SubstationGeoData;
import com.powsybl.sld.server.utils.GeoDataUtils;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class GeoDataService {

    public static final String QUERY_PARAM_SUBSTATION_ID = "substationId";
    public static final String GEO_DATA_API_VERSION = "v1";
    public static final String QUERY_PARAM_VARIANT_ID = "variantId";
    public static final String NETWORK_UUID = "networkUuid";
    static final String QUERY_PARAM_LINE_ID = "lineId";
    static final String LINES = "lines";
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

    public String getLinesGraphics(UUID networkUuid, String variantId, List<String> linesIds) {
        UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(getGeoDataServerURI() + LINES)
                .queryParam(NETWORK_UUID, networkUuid);

        if (!StringUtils.isBlank(variantId)) {
            uriComponentsBuilder.queryParam(QUERY_PARAM_VARIANT_ID, variantId);
        }

        if (linesIds != null) {
            uriComponentsBuilder.queryParam(QUERY_PARAM_LINE_ID, linesIds);
        }

        var path = uriComponentsBuilder
                .buildAndExpand()
                .toUriString();

        return restTemplate.getForObject(path, String.class);
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

    public void assignSubstationGeoData(Network network, UUID networkUuid, String variantId, List<Substation> substations) {

        List<SubstationGeoData> substationsGeoData = GeoDataUtils.fromStringToSubstationGeoData(getSubstationsGraphics(networkUuid, variantId, null), new ObjectMapper());
        Map<String, Coordinate> substationGeoDataMap = substationsGeoData.stream()
                .collect(Collectors.toMap(SubstationGeoData::getId, SubstationGeoData::getCoordinate));

        for (Substation substation : substations) {
            if (network.getSubstation(substation.getId()).getExtension(SubstationPosition.class) == null) {
                com.powsybl.sld.server.dto.Coordinate coordinate = substationGeoDataMap.get(substation.getId());
                if (coordinate != null) {
                    network.getSubstation(substation.getId())
                            .newExtension(SubstationPositionAdder.class)
                            .withCoordinate(new com.powsybl.iidm.network.extensions.Coordinate(coordinate.getLat(), coordinate.getLon()))
                            .add();
                }
            }

        }
    }

    public void assignLineGeoData(Network network, UUID networkUuid, String variantId, List<Line> lines) {
        List<LineGeoData> linesGeoData = GeoDataUtils.fromStringToLineGeoData(getLinesGraphics(networkUuid, variantId, null), new ObjectMapper());
        Map<String, List<com.powsybl.sld.server.dto.Coordinate>> lineGeoDataMap = linesGeoData.stream()
                .collect(Collectors.toMap(LineGeoData::getId, LineGeoData::getCoordinates));
        for (Line line : lines) {
            if (network.getLine(line.getId()).getExtension(LinePosition.class) == null) {
                List<com.powsybl.sld.server.dto.Coordinate> coordinates = lineGeoDataMap.get(line.getId());
                if (coordinates != null) {
                    network.getLine(line.getId())
                            .newExtension(LinePositionAdder.class)
                            .withCoordinates(coordinates)
                            .add();
                }
            }
        }
    }
}


