/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.RawValue;
import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.SubstationPosition;
import com.powsybl.iidm.network.extensions.SubstationPositionAdder;
import com.powsybl.nad.NadParameters;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.build.iidm.VoltageLevelFilter;
import com.powsybl.nad.layout.BasicForceLayout;
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
import lombok.Getter;
import lombok.Setter;
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
    private static final int SCALING_FACTOR = 450000;
    private static final int SCALING_FACTOR_TO_TEST = 1000;
    private static final double RADIUS_FACTOR = 300;

    static final String SVG_TAG = "svg";
    static final String METADATA = "metadata";
    static final String ADDITIONAL_METADATA = "additionalMetadata";

    private final NetworkStoreService networkStoreService;
    private final GeoDataService geoDataService;
    private final NetworkAreaExecutionService diagramExecutionService;

    private final ObjectMapper objectMapper;

    @Setter
    @Getter
    private double density = 0;

    public NetworkAreaDiagramService(NetworkStoreService networkStoreService, GeoDataService geoDataService,
                                     NetworkAreaExecutionService diagramExecutionService, ObjectMapper objectMapper) {
        this.networkStoreService = networkStoreService;
        this.geoDataService = geoDataService;
        this.diagramExecutionService = diagramExecutionService;
        this.objectMapper = objectMapper;
    }

    public String getNetworkAreaDiagramSvgAsync(UUID networkUuid, String variantId, List<String> voltageLevelsIds, int depth, boolean withGeoData) {
        return diagramExecutionService
            .supplyAsync(() -> getNetworkAreaDiagramSvg(networkUuid, variantId, voltageLevelsIds, depth, withGeoData))
            .join();
    }

    private String getNetworkAreaDiagramSvg(UUID networkUuid, String variantId, List<String> voltageLevelsIds, int depth, boolean withGeoData) {
        try {
            SvgAndMetadata svgAndMetadata = generateNetworkAreaDiagramSvg(networkUuid, variantId, voltageLevelsIds, depth, withGeoData);
            String svg = svgAndMetadata.getSvg();
            String metadata = svgAndMetadata.getMetadata();
            Object additionalMetadata = svgAndMetadata.getAdditionalMetadata();
            return objectMapper.writeValueAsString(
                objectMapper.createObjectNode()
                    .put(SVG_TAG, svg)
                    .putRawValue(METADATA, new RawValue(metadata))
                    .putPOJO(ADDITIONAL_METADATA, additionalMetadata));
        } catch (JsonProcessingException e) {
            throw new PowsyblException("Failed to parse JSON response", e);
        }
    }

    private double calculateHypotenuse(double a, double b) {
        return Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
    }
    private double modifiedSigmoid(double x) {
        // See the curve : https://www.google.com/search?q=1%2F(1%2Be%5E(8(x-0.5)))
        return 1.0 / (1.0 + Math.exp(8.0 * (x - 0.5)));
    }

    public int getScalingFactor(int charlyDepth) {
        /*System.out.println("CHARLY 🐞 getMaxDeltaForScale => "+this.getMaxDeltaForScale());
        System.out.println("CHARLY 🐞 modifiedSigmoid => "+this.modifiedSigmoid(this.getMaxDeltaForScale()));
        int result = (int) Math.round( this.modifiedSigmoid(this.getMaxDeltaForScale() / 5) * (MAX_SCALING_FACTOR - MIN_SCALING_FACTOR) + MIN_SCALING_FACTOR);
        System.out.println("CHARLY 🐞🐞 getScalingFactor => " + result);
        return result;*/
        if (charlyDepth != 0) {
            System.out.println("🪲 charlyDepth SCALING_FACTOR => "+charlyDepth);
            return charlyDepth;
        }
        if ("charly".length() > 2) {
            System.out.println("🪲 default value SCALING_FACTOR => "+SCALING_FACTOR);
            return SCALING_FACTOR;
        }

        int result = SCALING_FACTOR;
        if (this.getDensity() != 0) {
            result = (int) Math.round(SCALING_FACTOR * this.getDensity());
            System.out.println("🪲 SCALING_FACTOR => "+SCALING_FACTOR);
            System.out.println("🦂 getScalingFactor => "+result);
        }
        return result;
    }

    public double getRadiusFactor() {
        /*if (this.getMaxDeltaForScale() != 0) {
            double result = RADIUS_FACTOR * 0.3 * this.getMaxDeltaForScale();
            //double result = RADIUS_FACTOR * this.modifiedSigmoid(this.getMaxDeltaForScale()) * this.getMaxDeltaForScale();
            // TODO CHARLY division par zéro ?
            System.out.println("CHARLY 🐞🐞🐞 getRadiusFactor => " + Math.round(result));
            return result;
        }*/
        return RADIUS_FACTOR;
    }

    private void calculateScalingFactor(Collection<Coordinate> coordinates) {
        // Used to find the range of values
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;

        // Used to find the density of values
        //Map<String, Integer> gridCounts = new HashMap<>();

        for (Coordinate coordinate : coordinates) {
            //System.out.println("🐞 Longitude: " + coordinate.getLon() + ", Latitude: " + coordinate.getLat());

            double lat = coordinate.getLat();
            double lon = coordinate.getLon();

            if (lat < minLat) { minLat = lat; }
            if (lat > maxLat) { maxLat = lat; }
            if (lon < minLon) { minLon = lon; }
            if (lon > maxLon) { maxLon = lon; }

            // Calculate the grid cell indices
            //int latIndex = (int) Math.floor(lat);
            //int lonIndex = (int) Math.floor(lon);

            // Create a unique key for the grid cell
            //String gridKey = latIndex + "_" + lonIndex;

            // Increment the count for this grid cell
            //gridCounts.put(gridKey, gridCounts.getOrDefault(gridKey, 0) + 1);
        }

        /*for (Map.Entry<String, Integer> entry : gridCounts.entrySet()) {
            System.out.println("🪲 Grid: " + entry.getKey() + ", Count: " + entry.getValue());
        }*/

        // Represent the size reference used to calculate the density
        double diagonal = calculateHypotenuse(maxLon - minLon, maxLat - minLat);
        double width = maxLat - minLat;
        double height = maxLon - minLon;
        this.setDensity(coordinates.size() / (width * height));

        System.out.println("🐝 diagonal: " + diagonal);
        System.out.println("🐞 density: " + this.getDensity());
    }

    public SvgAndMetadata generateNetworkAreaDiagramSvg(UUID networkUuid, String variantId, List<String> voltageLevelsIds, int charlyDepth, boolean withGeoData) {

        int depth = 0;
        Network network = DiagramUtils.getNetwork(networkUuid, variantId, networkStoreService, PreloadingStrategy.COLLECTION);
        List<String> existingVLIds = voltageLevelsIds.stream().filter(vl -> network.getVoltageLevel(vl) != null).toList();
        if (existingVLIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no voltage level was found");
        }
        try (StringWriter svgWriter = new StringWriter(); StringWriter metadataWriter = new StringWriter()) {
            SvgParameters svgParameters = new SvgParameters()
                    .setUndefinedValueSymbol("\u2014")
                    .setSvgWidthAndHeightAdded(true)
                    .setCssLocation(SvgParameters.CssLocation.EXTERNAL_NO_IMPORT);

            //List of selected voltageLevels with depth
            VoltageLevelFilter vlFilter = VoltageLevelFilter.createVoltageLevelsDepthFilter(network, existingVLIds, depth);

            LayoutParameters layoutParameters = new LayoutParameters();
            NadParameters nadParameters = new NadParameters();
            nadParameters.setSvgParameters(svgParameters);
            nadParameters.setLayoutParameters(layoutParameters);

            //Initialize with geographical data
            if (withGeoData) {
                //get voltage levels' positions on depth+1 to be able to locate lines on depth
                List<VoltageLevel> voltageLevels = VoltageLevelFilter.createVoltageLevelsDepthFilter(network, existingVLIds, depth + 1).getVoltageLevels().stream().toList();
                Map<String, Coordinate> substationGeoDataMap = assignGeoDataCoordinates(network, networkUuid, variantId, voltageLevels);
                calculateScalingFactor(substationGeoDataMap.values());
                nadParameters.setLayoutFactory(new GeographicalLayoutFactory(network, this.getScalingFactor(charlyDepth), this.getRadiusFactor(), BasicForceLayout::new));

            }
            nadParameters.setStyleProviderFactory(n -> new TopologicalStyleProvider(network));

            NetworkAreaDiagram.draw(network, svgWriter, metadataWriter, nadParameters, vlFilter);
            Map<String, Object> additionalMetadata = computeAdditionalMetadata(network, existingVLIds, depth);

            return SvgAndMetadata.builder()
                    .svg(svgWriter.toString())
                    .metadata(metadataWriter.toString())
                    .additionalMetadata(additionalMetadata).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Map<String, Coordinate> assignGeoDataCoordinates(Network network, UUID networkUuid, String variantId, List<VoltageLevel> voltageLevels) {
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

        return substationGeoDataMap;
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
