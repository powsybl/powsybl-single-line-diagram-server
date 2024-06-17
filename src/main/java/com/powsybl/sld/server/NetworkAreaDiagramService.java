/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.SubstationPosition;
import com.powsybl.iidm.network.extensions.SubstationPositionAdder;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.layout.GeographicalLayoutFactory;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.server.dto.Coordinate;
import com.powsybl.sld.server.dto.SubstationGeoData;
import com.powsybl.sld.server.dto.SvgAndMetadata;
import com.powsybl.sld.server.dto.VoltageLevelInfos;
import com.powsybl.sld.server.utils.DiagramUtils;
import com.powsybl.sld.server.utils.GeoDataUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer<etienne.homer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
class NetworkAreaDiagramService {
    @Autowired
    private NetworkStoreService networkStoreService;

    @Autowired
    private GeoDataService geoDataService;

    public SvgAndMetadata generateNetworkAreaDiagramSvg(UUID networkUuid, String variantId, List<String> voltageLevelsIds, int depth) {
        Network network = DiagramUtils.getNetwork(networkUuid, variantId, networkStoreService, PreloadingStrategy.COLLECTION);
        List<String> existingVLIds = voltageLevelsIds.stream().filter(vl -> network.getVoltageLevel(vl) != null).toList();
        if (existingVLIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no voltage level was found");
        }
        try (StringWriter svgWriter = new StringWriter()) {
            SvgParameters svgParameters = new SvgParameters()
                    .setSvgWidthAndHeightAdded(true)
                    .setCssLocation(SvgParameters.CssLocation.EXTERNAL_NO_IMPORT);

            //List of selected voltageLevels with depth
            VoltageLevelFilter vlFilter = VoltageLevelFilter.createVoltageLevelsDepthFilter(network, existingVLIds, depth);
            //get voltage levels' positions on depth+1 to be able to locate lines on depth
            List<VoltageLevel> voltageLevels = VoltageLevelFilter.createVoltageLevelsDepthFilter(network, existingVLIds, depth + 1).getVoltageLevels().stream().toList();
            assignGeoDataCoordinates(network, networkUuid, variantId, voltageLevels);

            LayoutParameters layoutParameters = new LayoutParameters();
            NadParameters nadParameters = new NadParameters();
            nadParameters.setSvgParameters(svgParameters);
            nadParameters.setLayoutParameters(layoutParameters);
            nadParameters.setLayoutFactory(new GeographicalLayoutFactory(network));
            nadParameters.setStyleProviderFactory(n -> new TopologicalStyleProvider(network));

            NetworkAreaDiagram.draw(network, svgWriter, nadParameters, vlFilter);
            Map<String, Object> additionalMetadata = computeAdditionalMetadata(network, existingVLIds, depth);

            return SvgAndMetadata.builder()
                    .svg(svgWriter.toString())
                    .additionalMetadata(additionalMetadata).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void assignGeoDataCoordinates(Network network, UUID networkUuid, String variantId, List<VoltageLevel> voltageLevels) {
        // Geographical positions for substations related to voltageLevels
        List<Substation> substations = voltageLevels.stream()
                .map(VoltageLevel::getNullableSubstation)
                .filter(Objects::nonNull)
                .toList();

        String substationsGeoDataString = geoDataService.getSubstationsGraphics(networkUuid, variantId, substations.stream().map(Substation::getId).toList());
        List<SubstationGeoData> substationsGeoData = GeoDataUtils.fromStringToSubstationGeoData(substationsGeoDataString, new ObjectMapper());
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

    private Map<String, Object> computeAdditionalMetadata(Network network, List<String> voltageLevelsIds, int depth) {

        VoltageLevelFilter vlFilter = VoltageLevelFilter.createVoltageLevelsDepthFilter(network, voltageLevelsIds, depth);

        List<VoltageLevelInfos> voltageLevelsInfos = voltageLevelsIds.stream()
                .map(network::getVoltageLevel)
                .map(VoltageLevelInfos::new)
                .toList();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("nbVoltageLevels", vlFilter.getNbVoltageLevels());
        metadata.put("voltageLevels", voltageLevelsInfos);

        return metadata;
    }
}
