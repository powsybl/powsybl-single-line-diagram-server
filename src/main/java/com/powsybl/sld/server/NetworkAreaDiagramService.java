/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.*;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.layout.GeographicalLayoutFactory;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.server.dto.LineGeoData;
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
import java.util.stream.StreamSupport;

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
            //get more infos (depth+1) to locate voltages, substation and lines on depth
            Set<VoltageLevel> voltageLevels = VoltageLevelFilter.createVoltageLevelsDepthFilter(network, existingVLIds, depth + 1).getVoltageLevels();
            getGeoDataCoordinates(network, networkUuid, variantId, new ArrayList<>(voltageLevels));

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

    private void getGeoDataCoordinates(Network network, UUID networkUuid, String variantId, List<VoltageLevel> voltageLevels) {
        // Geographical Position for lines and substations related to voltageLevels
        List<Substation> substations = getSubstations(voltageLevels);
        assignSubstationGeoData(network, networkUuid, variantId, substations);
        assignLineGeoData(network, networkUuid, variantId, getLines(voltageLevels));
    }

    private void assignSubstationGeoData(Network network, UUID networkUuid, String variantId, List<Substation> substations) {

        List<SubstationGeoData> substationsGeoData = GeoDataUtils.fromStringToSubstationGeoData(geoDataService.getSubstationsGraphics(networkUuid, variantId, null), new ObjectMapper());
        Map<String, com.powsybl.sld.server.dto.Coordinate> substationGeoDataMap = substationsGeoData.stream()
                .collect(Collectors.toMap(SubstationGeoData::getId, SubstationGeoData::getCoordinate));

        for (Substation substation : substations) {
            if (network.getSubstation(substation.getId()).getExtension(SubstationPosition.class) == null) {
                com.powsybl.sld.server.dto.Coordinate coordinate = substationGeoDataMap.get(substation.getId());
                if (coordinate != null) {
                    network.getSubstation(substation.getId())
                            .newExtension(SubstationPositionAdder.class)
                            .withCoordinate(new Coordinate(coordinate.getLat(), coordinate.getLon()))
                            .add();
                }
            }

        }
    }

    private void assignLineGeoData(Network network, UUID networkUuid, String variantId, List<Line> lines) {
        List<LineGeoData> linesGeoData = GeoDataUtils.fromStringToLineGeoData(geoDataService.getLinesGraphics(networkUuid, variantId, null), new ObjectMapper());
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

    private List<Substation> getSubstations(List<VoltageLevel> voltages) {
        return voltages.stream()
                .map(VoltageLevel::getNullableSubstation)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Line> getLines(List<VoltageLevel> voltages) {
        return voltages.stream()
                .flatMap(voltageLevel -> StreamSupport.stream(voltageLevel.getLines().spliterator(), false))
                .toList();
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
