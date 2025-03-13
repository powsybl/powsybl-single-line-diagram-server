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
import com.powsybl.nad.layout.*;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.server.dto.Coordinate;
import com.powsybl.sld.server.dto.SubstationGeoData;
import com.powsybl.sld.server.dto.SvgAndMetadata;
import com.powsybl.sld.server.dto.VoltageLevelInfos;
import com.powsybl.sld.server.dto.nad.NadConfigInfos;
import com.powsybl.sld.server.dto.nad.NadVoltageLevelPositionInfos;
import com.powsybl.sld.server.entities.nad.NadConfigEntity;
import com.powsybl.sld.server.entities.nad.NadVoltageLevelPositionEntity;
import com.powsybl.sld.server.repository.NadConfigRepository;
import com.powsybl.sld.server.utils.DiagramUtils;
import com.powsybl.sld.server.utils.GeoDataUtils;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import com.powsybl.nad.model.Point;

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
    private static final int DEFAULT_SCALING_FACTOR = 450000;
    private static final int MIN_SCALING_FACTOR = 50000;
    private static final int MAX_SCALING_FACTOR = 600000;
    private static final double RADIUS_FACTOR = 300;

    static final String SVG_TAG = "svg";
    static final String METADATA = "metadata";
    static final String ADDITIONAL_METADATA = "additionalMetadata";

    private final NetworkStoreService networkStoreService;
    private final GeoDataService geoDataService;
    private final NetworkAreaExecutionService diagramExecutionService;

    private final NadConfigRepository nadConfigRepository;
    private final NetworkAreaDiagramService self;

    private final ObjectMapper objectMapper;

    public NetworkAreaDiagramService(NetworkStoreService networkStoreService,
                                     GeoDataService geoDataService,
                                     NetworkAreaExecutionService diagramExecutionService,
                                     NadConfigRepository nadConfigRepository,
                                     @Lazy NetworkAreaDiagramService networkAreaDiagramService,
                                     ObjectMapper objectMapper) {
        this.networkStoreService = networkStoreService;
        this.geoDataService = geoDataService;
        this.diagramExecutionService = diagramExecutionService;
        this.nadConfigRepository = nadConfigRepository;
        this.self = networkAreaDiagramService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID createNetworkAreaDiagramConfig(NadConfigInfos nadConfigInfos) {
        // TODO At the moment, it is possible to insert multiple positions with the same voltageLevelId. That should probably be fixed.
        return nadConfigRepository.save(nadConfigInfos.toEntity()).getId();
    }

    @Transactional
    public UUID duplicateNetworkAreaDiagramConfig(UUID originNadConfigUuid) {
        Optional<NadConfigEntity> nadConfigEntityOpt = nadConfigRepository.findById(originNadConfigUuid);
        if (nadConfigEntityOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        NadConfigEntity nadConfigEntityToDuplicate = nadConfigEntityOpt.get();
        return nadConfigRepository.save(new NadConfigEntity(nadConfigEntityToDuplicate)).getId();
    }

    @Transactional
    public void updateNetworkAreaDiagramConfig(UUID nadConfigUuid, NadConfigInfos nadConfigInfos) {
        NadConfigEntity entity = nadConfigRepository.findWithVoltageLevelIdsById(nadConfigUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        updateNadConfig(entity, nadConfigInfos);
    }

    private void updateNadConfig(@NonNull NadConfigEntity entity, @NonNull NadConfigInfos nadConfigInfos) {
        Optional.ofNullable(nadConfigInfos.getVoltageLevelIds()).ifPresent(voltageLevels ->
            entity.setVoltageLevelIds(new ArrayList<>(voltageLevels))
        );
        Optional.ofNullable(nadConfigInfos.getDepth()).ifPresent(entity::setDepth);
        Optional.ofNullable(nadConfigInfos.getScalingFactor()).ifPresent(entity::setScalingFactor);
        Optional.ofNullable(nadConfigInfos.getRadiusFactor()).ifPresent(entity::setRadiusFactor);

        if (nadConfigInfos.getPositions() != null && !nadConfigInfos.getPositions().isEmpty()) {
            updatePositions(entity, nadConfigInfos);
        }
    }

    private void updatePositions(@NonNull NadConfigEntity entity, @NonNull NadConfigInfos nadConfigInfos) {
        // Build two lookup maps in a single iteration for better performance.
        Map<UUID, NadVoltageLevelPositionEntity> uuidPositionsMap = new HashMap<>();
        Map<String, NadVoltageLevelPositionEntity> voltageLevelIdPositionsMap = new HashMap<>();
        for (NadVoltageLevelPositionEntity position : entity.getPositions()) {
            uuidPositionsMap.put(position.getId(), position);
            voltageLevelIdPositionsMap.put(position.getVoltageLevelId(), position);
        }

        for (NadVoltageLevelPositionInfos info : nadConfigInfos.getPositions()) {
            if ((info.getId() == null || !uuidPositionsMap.containsKey(info.getId())) && info.getVoltageLevelId() == null) {
                throw new IllegalArgumentException("Missing id or voltageLevelId");
            }
            if (voltageLevelIdPositionsMap.containsKey(info.getVoltageLevelId())) {
                updateVoltageLevelPositions(voltageLevelIdPositionsMap.get(info.getVoltageLevelId()), info);
            } else if (info.getId() != null && uuidPositionsMap.containsKey(info.getId())) {
                updateVoltageLevelPositions(uuidPositionsMap.get(info.getId()), info);
            } else {
                NadVoltageLevelPositionEntity newPosition = info.toEntity();
                entity.getPositions().add(newPosition);
                // We add the newly added position to the map to ensure we don't try to create another position with the same voltageLevelId
                voltageLevelIdPositionsMap.put(info.getVoltageLevelId(), newPosition);
            }
        }
    }

    private void updateVoltageLevelPositions(@NonNull NadVoltageLevelPositionEntity entity, @NonNull NadVoltageLevelPositionInfos nadVoltageLevelPositionInfos) {
        Optional.ofNullable(nadVoltageLevelPositionInfos.getVoltageLevelId()).ifPresent(entity::setVoltageLevelId);
        Optional.ofNullable(nadVoltageLevelPositionInfos.getXPosition()).ifPresent(entity::setXPosition);
        Optional.ofNullable(nadVoltageLevelPositionInfos.getYPosition()).ifPresent(entity::setYPosition);
        Optional.ofNullable(nadVoltageLevelPositionInfos.getXLabelPosition()).ifPresent(entity::setXLabelPosition);
        Optional.ofNullable(nadVoltageLevelPositionInfos.getYLabelPosition()).ifPresent(entity::setYLabelPosition);
    }

    @Transactional(readOnly = true)
    public NadConfigInfos getNetworkAreaDiagramConfig(UUID nadConfigUuid) {
        return nadConfigRepository.findWithVoltageLevelIdsById(nadConfigUuid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)).toDto();
    }

    @Transactional
    public void deleteNetworkAreaDiagramConfig(UUID nadConfigUuid) {
        nadConfigRepository.deleteById(nadConfigUuid);
    }

    public String loadNetworkAreaDiagramSvgFromConfigAsync(UUID networkUuid, String variantId, UUID nadConfigUuid) {
        return diagramExecutionService
                .supplyAsync(() -> self.getNetworkAreaDiagramConfig(nadConfigUuid))
                .thenApply(nadConfigInfos -> processSvgAndMetadata(loadNetworkAreaDiagramSvg(networkUuid, variantId, nadConfigInfos)))
                .join();
    }

    public String generateNetworkAreaDiagramSvgAsync(UUID networkUuid, String variantId, List<String> voltageLevelsIds, int depth, boolean withGeoData) {
        return diagramExecutionService
                .supplyAsync(() -> processSvgAndMetadata(generateNetworkAreaDiagramSvg(networkUuid, variantId, voltageLevelsIds, depth, withGeoData)))
                .join();
    }

    private String processSvgAndMetadata(SvgAndMetadata svgAndMetadata) {
        try {
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

    private int calculateScalingFactor(Collection<Coordinate> coordinates) {
        if (coordinates.isEmpty()) {
            return DEFAULT_SCALING_FACTOR;
        }
        double density = calculateDensity(coordinates);
        // The value 15700 was tested to give good results across various real-world cases.
        int result = (int) Math.round(15700 * density + MIN_SCALING_FACTOR);
        if (result > MAX_SCALING_FACTOR) {
            result = MAX_SCALING_FACTOR;
        }
        if (result < MIN_SCALING_FACTOR) {
            result = MIN_SCALING_FACTOR;
        }
        return result;
    }

    private double calculateDensity(Collection<Coordinate> coordinates) {
        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;
        double gridSize = 0.5;
        for (Coordinate coordinate : coordinates) {
            double lat = coordinate.getLat();
            double lon = coordinate.getLon();
            if (lat < minLat) {
                minLat = lat;
            }
            if (lat > maxLat) {
                maxLat = lat;
            }
            if (lon < minLon) {
                minLon = lon;
            }
            if (lon > maxLon) {
                maxLon = lon;
            }
        }
        double width = Math.floor(maxLat / gridSize) - Math.floor(minLat / gridSize) + gridSize;
        double height = Math.floor(maxLon / gridSize) - Math.floor(minLon / gridSize) + gridSize;
        return coordinates.size() / (width * height);
    }

    public SvgAndMetadata loadNetworkAreaDiagramSvg(@NotNull UUID networkUuid, String variantId, NadConfigInfos nadConfigInfos) {
        Network network = DiagramUtils.getNetwork(networkUuid, variantId, networkStoreService, PreloadingStrategy.COLLECTION);
        List<String> existingVLIds = nadConfigInfos.getVoltageLevelIds().stream().filter(vl -> network.getVoltageLevel(vl) != null).toList();
        if (existingVLIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no voltage level was found");
        }

        SvgParameters svgParameters = new SvgParameters()
                .setUndefinedValueSymbol("\u2014")
                .setSvgWidthAndHeightAdded(true)
                .setCssLocation(SvgParameters.CssLocation.EXTERNAL_NO_IMPORT);

        //List of selected voltageLevels with depth
        VoltageLevelFilter vlFilter = VoltageLevelFilter.createVoltageLevelsDepthFilter(network, existingVLIds, nadConfigInfos.getDepth());

        LayoutParameters layoutParameters = new LayoutParameters();
        NadParameters nadParameters = new NadParameters();
        nadParameters.setSvgParameters(svgParameters);
        nadParameters.setLayoutParameters(layoutParameters);
        nadParameters.setStyleProviderFactory(n -> new TopologicalStyleProvider(network));

        Map<String, Point> positionsForFixedLayout = nadConfigInfos.getPositions().stream()
            .collect(Collectors.toMap(
                NadVoltageLevelPositionInfos::getVoltageLevelId,
                info -> new Point(info.getXPosition(), info.getYPosition())
            ));

        LayoutFactory layoutFactory = new FixedLayoutFactory(positionsForFixedLayout, BasicForceLayout::new);
        nadParameters.setLayoutFactory(layoutFactory);

        return drawSvgAndBuildMetadata(network, nadParameters, vlFilter, existingVLIds, nadConfigInfos.getDepth(), nadConfigInfos.getScalingFactor());
    }

    public SvgAndMetadata generateNetworkAreaDiagramSvg(UUID networkUuid, String variantId, List<String> voltageLevelsIds, int depth, boolean withGeoData) {
        Network network = DiagramUtils.getNetwork(networkUuid, variantId, networkStoreService, PreloadingStrategy.COLLECTION);
        List<String> existingVLIds = voltageLevelsIds.stream().filter(vl -> network.getVoltageLevel(vl) != null).toList();
        if (existingVLIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no voltage level was found");
        }

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
        int scalingFactor = 0;
        if (withGeoData) {
            List<VoltageLevel> voltageLevels = vlFilter.getVoltageLevels().stream().toList();
            List<String> substations = voltageLevels.stream()
                    .map(VoltageLevel::getNullableSubstation)
                    .filter(Objects::nonNull)
                    .map(Substation::getId)
                    .toList();

            //get voltage levels' positions on depth+1 to be able to locate lines on depth
            List<VoltageLevel> voltageLevelsPlusOneDepth = VoltageLevelFilter.createVoltageLevelsDepthFilter(network, existingVLIds, depth + 1).getVoltageLevels().stream().toList();
            Map<String, Coordinate> substationGeoDataMap = assignGeoDataCoordinates(network, networkUuid, variantId, voltageLevelsPlusOneDepth);

            // We only keep the depth+0 voltage levels' positions to calculate the scaling factor
            List<Coordinate> coordinatesForScaling = substationGeoDataMap.entrySet().stream()
                    .filter(entry -> substations.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .toList();
            scalingFactor = this.calculateScalingFactor(coordinatesForScaling);

            nadParameters.setLayoutFactory(new GeographicalLayoutFactory(network, scalingFactor, RADIUS_FACTOR, BasicForceLayout::new));
        }
        nadParameters.setStyleProviderFactory(n -> new TopologicalStyleProvider(network));

        return drawSvgAndBuildMetadata(network, nadParameters, vlFilter, existingVLIds, depth, scalingFactor);
    }

    private SvgAndMetadata drawSvgAndBuildMetadata(Network network, NadParameters nadParameters, VoltageLevelFilter vlFilter, List<String> existingVLIds, int depth, int scalingFactor) {
        try (StringWriter svgWriter = new StringWriter(); StringWriter metadataWriter = new StringWriter()) {
            NetworkAreaDiagram.draw(network, svgWriter, metadataWriter, nadParameters, vlFilter);
            Map<String, Object> additionalMetadata = computeAdditionalMetadata(network, existingVLIds, depth, scalingFactor);

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

    private Map<String, Object> computeAdditionalMetadata(Network network, List<String> voltageLevelsIds, int depth, int scalingFactor) {

        VoltageLevelFilter vlFilter = VoltageLevelFilter.createVoltageLevelsDepthFilter(network, voltageLevelsIds, depth);

        List<VoltageLevelInfos> voltageLevelsInfos = voltageLevelsIds.stream()
                .map(network::getVoltageLevel)
                .map(VoltageLevelInfos::new)
                .toList();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("nbVoltageLevels", vlFilter.getNbVoltageLevels());
        metadata.put("voltageLevels", voltageLevelsInfos);
        metadata.put("scalingFactor", scalingFactor);

        return metadata;
    }
}
