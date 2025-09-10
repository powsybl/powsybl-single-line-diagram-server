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
import com.powsybl.nad.model.Point;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.server.dto.*;
import com.powsybl.sld.server.dto.nad.NadConfigInfos;
import com.powsybl.sld.server.dto.nad.NadGenerationContext;
import com.powsybl.sld.server.dto.nad.NadRequestInfos;
import com.powsybl.sld.server.dto.nad.NadVoltageLevelPositionInfos;
import com.powsybl.sld.server.entities.nad.NadConfigEntity;
import com.powsybl.sld.server.entities.nad.NadVoltageLevelConfiguredPositionEntity;
import com.powsybl.sld.server.entities.nad.NadVoltageLevelPositionEntity;
import com.powsybl.sld.server.repository.NadConfigRepository;
import com.powsybl.sld.server.repository.NadVoltageLevelConfiguredPositionRepository;
import com.powsybl.sld.server.utils.DiagramUtils;
import com.powsybl.sld.server.utils.CsvFileValidator;
import com.powsybl.sld.server.utils.NadPositionsGenerationMode;
import com.powsybl.sld.server.utils.ResourceUtils;
import lombok.NonNull;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.io.CsvMapReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Etienne Homer<etienne.homer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
class NetworkAreaDiagramService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkAreaDiagramService.class);

    @Value("${diagram-server.nad.max-voltage-levels}")
    private int maxVoltageLevels;

    private static final int DEFAULT_SCALING_FACTOR = 450000;
    private static final int MIN_SCALING_FACTOR = 50000;
    private static final int MAX_SCALING_FACTOR = 600000;
    private static final double RADIUS_FACTOR = 300;

    static final String SVG_TAG = "svg";
    static final String METADATA = "metadata";
    static final String ADDITIONAL_METADATA = "additionalMetadata";

    private final NetworkStoreService networkStoreService;
    private final GeoDataService geoDataService;
    private final FilterService filterService;
    private final NetworkAreaExecutionService diagramExecutionService;

    private final NadConfigRepository nadConfigRepository;
    private final NadVoltageLevelConfiguredPositionRepository nadVoltageLevelConfiguredPositionRepository;
    private final NetworkAreaDiagramService self;

    private final ObjectMapper objectMapper;

    public NetworkAreaDiagramService(NetworkStoreService networkStoreService,
                                     GeoDataService geoDataService,
                                     FilterService filterService,
                                     NetworkAreaExecutionService diagramExecutionService,
                                     NadConfigRepository nadConfigRepository,
                                     NadVoltageLevelConfiguredPositionRepository nadVoltageLevelConfiguredPositionRepository,
                                     @Lazy NetworkAreaDiagramService networkAreaDiagramService,
                                     ObjectMapper objectMapper) {
        this.networkStoreService = networkStoreService;
        this.geoDataService = geoDataService;
        this.filterService = filterService;
        this.diagramExecutionService = diagramExecutionService;
        this.nadConfigRepository = nadConfigRepository;
        this.nadVoltageLevelConfiguredPositionRepository = nadVoltageLevelConfiguredPositionRepository;
        this.self = networkAreaDiagramService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public UUID createNetworkAreaDiagramConfig(NadConfigInfos nadConfigInfos) {
        return nadConfigRepository.save(nadConfigInfos.toEntity()).getId();
    }

    @Transactional
    public List<UUID> createNetworkAreaDiagramConfigs(List<NadConfigInfos> nadConfigs) {
        List<NadConfigEntity> configs = nadConfigs.stream()
            .map(NadConfigInfos::toEntity)
            .toList();

        List<NadConfigEntity> savedConfigs = nadConfigRepository.saveAll(configs);
        return savedConfigs.stream()
            .map(NadConfigEntity::getId)
            .toList();
    }

    @Transactional
    public void deleteNetworkAreaDiagramConfigs(List<UUID> configUuids) {
        nadConfigRepository.deleteAllById(configUuids);
    }

    @Transactional
    public UUID duplicateNetworkAreaDiagramConfig(UUID originNadConfigUuid) {
        Optional<NadConfigEntity> nadConfigEntityOpt = nadConfigRepository.findById(originNadConfigUuid);
        if (nadConfigEntityOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        NadConfigEntity nadConfigEntityToDuplicate = nadConfigEntityOpt.get();
        NadConfigEntity duplicateEntity = new NadConfigEntity(nadConfigEntityToDuplicate);
        duplicateEntity.setId(UUID.randomUUID()); // Assign new ID for the duplicate
        return nadConfigRepository.save(duplicateEntity).getId();
    }

    @Transactional
    public void updateNetworkAreaDiagramConfig(UUID nadConfigUuid, NadConfigInfos nadConfigInfos) {
        NadConfigEntity entity = nadConfigRepository.findWithVoltageLevelIdsById(nadConfigUuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        updateNadConfig(entity, nadConfigInfos);
    }

    private void updateNadConfig(@NonNull NadConfigEntity entity, @NonNull NadConfigInfos nadConfigInfos) {
        Optional.ofNullable(nadConfigInfos.getVoltageLevelIds()).ifPresent(voltageLevels ->
            entity.setVoltageLevelIds(new HashSet<>(voltageLevels))
        );
        Optional.ofNullable(nadConfigInfos.getScalingFactor()).ifPresent(entity::setScalingFactor);

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

    private Set<String> getVoltageLevelIdsFromFilter(UUID networkUuid, String variantId, UUID filterUuid) {
        List<IdentifiableAttributes> filterContent = filterService.exportFilter(networkUuid, variantId, filterUuid);
        return filterContent.stream()
            .map(IdentifiableAttributes::getId)
            .collect(Collectors.toSet());
    }

    @Transactional
    public void deleteNetworkAreaDiagramConfig(UUID nadConfigUuid) {
        nadConfigRepository.deleteById(nadConfigUuid);
    }

    public String generateNetworkAreaDiagramSvgAsync(UUID networkUuid, String variantId, NadRequestInfos nadRequestInfos) {
        return diagramExecutionService.supplyAsync(() -> self.generateNetworkAreaDiagramSvg(networkUuid, variantId, nadRequestInfos)).join();
    }

    @Transactional(readOnly = true)
    public String generateNetworkAreaDiagramSvg(UUID networkUuid, String variantId, NadRequestInfos nadRequestInfos) {
        // Context setup
        NadGenerationContext nadGenerationContext = NadGenerationContext.builder()
            .networkUuid(networkUuid)
            .variantId(variantId)
            .network(DiagramUtils.getNetwork(networkUuid, variantId, networkStoreService, PreloadingStrategy.COLLECTION))
            // If we already have a NAD config UUID, it means the NAD was already generated,so we keep the automatic generation mode.
            .nadPositionsGenerationMode(nadRequestInfos.getNadConfigUuid() == null ? nadRequestInfos.getNadPositionsGenerationMode() : NadPositionsGenerationMode.AUTOMATIC)
            .positions(nadRequestInfos.getPositions())
            .build();

        // Positions config fetching
        if (nadRequestInfos.getNadConfigUuid() != null) {
            handleNadConfigPositions(nadRequestInfos.getNadConfigUuid(), nadGenerationContext);
        } else if (nadRequestInfos.getNadPositionsGenerationMode() == NadPositionsGenerationMode.CONFIGURED) {
            handleConfiguredPositions(nadGenerationContext);
        }

        // Filter fetching
        if (nadRequestInfos.getFilterUuid() != null) {
            nadGenerationContext.getVoltageLevelIds().addAll(getVoltageLevelIdsFromFilter(networkUuid, variantId, nadRequestInfos.getFilterUuid()));
        }

        // Manual user selection processing
        nadGenerationContext.getVoltageLevelIds().addAll(nadRequestInfos.getVoltageLevelIds());
        nadGenerationContext.getVoltageLevelIds().removeAll(nadRequestInfos.getVoltageLevelToOmitIds());
        if (!nadRequestInfos.getVoltageLevelToExpandIds().isEmpty()) {
            nadGenerationContext.getVoltageLevelIds().addAll(getExpandedVoltageLevelIds(nadRequestInfos.getVoltageLevelToExpandIds(), nadGenerationContext.getNetwork()));
        }

        // Filter out of scope voltage levels
        nadGenerationContext.setVoltageLevelIds(nadGenerationContext.getVoltageLevelIds().stream()
            .filter(vl -> nadGenerationContext.getNetwork().getVoltageLevel(vl) != null)
            .collect(Collectors.toSet()));
        nadGenerationContext.setVoltageLevelFilter(
                VoltageLevelFilter.createVoltageLevelsFilter(
                        nadGenerationContext.getNetwork(),
                        new ArrayList<>(nadGenerationContext.getVoltageLevelIds())
                )
        );

        // Build Powsybl parameters
        buildGraphicalParameters(nadGenerationContext);

        int nbVoltageLevels = nadGenerationContext.getVoltageLevelIds().size();
        if (nbVoltageLevels > maxVoltageLevels) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, String.format("You need to reduce the number of voltage levels to be displayed in the network area diagram (current %s, maximum %s)", nbVoltageLevels, maxVoltageLevels));
        }

        return processSvgAndMetadata(drawSvgAndBuildMetadata(nadGenerationContext));
    }

    private void buildGraphicalParameters(NadGenerationContext nadGenerationContext) {

        if (nadGenerationContext.getVoltageLevelIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "no voltage level was found");
        }

        SvgParameters svgParameters = new SvgParameters()
                .setUndefinedValueSymbol("\u2014")
                .setSvgWidthAndHeightAdded(true)
                .setCssLocation(SvgParameters.CssLocation.EXTERNAL_NO_IMPORT);

        LayoutParameters layoutParameters = new LayoutParameters();
        NadParameters nadParameters = new NadParameters();
        nadParameters.setSvgParameters(svgParameters);
        nadParameters.setLayoutParameters(layoutParameters);
        nadParameters.setStyleProviderFactory(n -> new TopologicalStyleProvider(nadGenerationContext.getNetwork()));

        // Set style provider factory either with geographical data or with the provided positions (if any)
        if (nadGenerationContext.getNadPositionsGenerationMode() == NadPositionsGenerationMode.GEOGRAPHICAL_COORDINATES && nadGenerationContext.getPositions().isEmpty()) {
            nadParameters.setLayoutFactory(prepareGeographicalLayoutFactory(nadGenerationContext));
        } else {
            nadParameters.setLayoutFactory(prepareFixedLayoutFactory(nadGenerationContext));
        }

        nadGenerationContext.setNadParameters(nadParameters);
    }

    private void handleNadConfigPositions(UUID nadConfigUuid, NadGenerationContext nadGenerationContext) {
        NadConfigInfos nadConfigInfos = getNetworkAreaDiagramConfig(nadConfigUuid);

        nadGenerationContext.getVoltageLevelIds().addAll(nadConfigInfos.getVoltageLevelIds());

        // Add voltage level positions that are not already present
        Set<String> existingVoltageLevelIds = nadGenerationContext.getPositions().stream()
            .map(NadVoltageLevelPositionInfos::getVoltageLevelId)
            .collect(Collectors.toSet());
        nadConfigInfos.getPositions().stream()
            .filter(position -> !existingVoltageLevelIds.contains(position.getVoltageLevelId()))
            .forEach(nadGenerationContext.getPositions()::add);

        nadGenerationContext.setScalingFactor(nadConfigInfos.getScalingFactor());
    }

    private void handleConfiguredPositions(NadGenerationContext nadGenerationContext) {
        // Add voltage level positions that are not already present
        Set<String> existingVoltageLevelIds = nadGenerationContext.getPositions().stream()
                .map(NadVoltageLevelPositionInfos::getVoltageLevelId)
                .collect(Collectors.toSet());

        // Get the VL positions that were previously saved using csv file.
        List<NadVoltageLevelConfiguredPositionEntity> nadVoltageLevelPositionInfos = nadVoltageLevelConfiguredPositionRepository.findAll();
        if (nadVoltageLevelPositionInfos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No configured positions found!");
        }
        nadVoltageLevelPositionInfos
                .stream()
                .filter(position -> !existingVoltageLevelIds.contains(position.getVoltageLevelId()))
                .map(NadVoltageLevelConfiguredPositionEntity::toDto)
                .forEach(nadGenerationContext.getPositions()::add);
    }

    private LayoutFactory prepareGeographicalLayoutFactory(NadGenerationContext nadGenerationContext) {

        // In order to draw half the lines that connect to the out of bound voltage levels, we have to know their coordinates.
        // To do so, we create a filter with a depth=1 that will include these out of bound voltage levels.
        List<VoltageLevel> extendedVoltageLevelFilter = VoltageLevelFilter.createVoltageLevelsDepthFilter(
                nadGenerationContext.getNetwork(),
                new ArrayList<>(nadGenerationContext.getVoltageLevelIds()),
                1).getVoltageLevels().stream().toList();

        List<Substation> extendedSubstations = extendedVoltageLevelFilter.stream()
                .map(VoltageLevel::getNullableSubstation)
                .filter(Objects::nonNull)
                .toList();

        // Watch out : assignGeoDataCoordinates also modifies the network
        Map<String, Coordinate> substationGeoDataMap = assignGeoDataCoordinates(nadGenerationContext, extendedSubstations);

        if (nadGenerationContext.getScalingFactor() == null || nadGenerationContext.getScalingFactor() <= 0) {
            // Let's calculate the scaling factor
            List<String> substations = nadGenerationContext.getVoltageLevelFilter().getVoltageLevels().stream()
                            .map(VoltageLevel::getNullableSubstation)
                            .filter(Objects::nonNull)
                            .map(Substation::getId)
                            .toList();

            List<Coordinate> coordinatesForScaling = substationGeoDataMap.entrySet().stream()
                    .filter(entry -> substations.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .toList();

            nadGenerationContext.setScalingFactor(this.calculateScalingFactor(coordinatesForScaling));
        }
        return new GeographicalLayoutFactory(nadGenerationContext.getNetwork(), nadGenerationContext.getScalingFactor(), RADIUS_FACTOR, BasicForceLayout::new);
    }

    private LayoutFactory prepareFixedLayoutFactory(NadGenerationContext nadGenerationContext) {
        Map<String, Point> positionsForFixedLayout = new HashMap<>();
        Map<String, TextPosition> textNodesPositionsForFixedLayout = new HashMap<>();

        nadGenerationContext.getPositions().forEach(info -> {
            positionsForFixedLayout.put(
                    info.getVoltageLevelId(),
                    new Point(info.getXPosition(), info.getYPosition())
            );
            textNodesPositionsForFixedLayout.put(
                    info.getVoltageLevelId(),
                    new TextPosition(
                            new Point(info.getXLabelPosition(), info.getYLabelPosition()),
                            new Point(0, 0) // We do not display the edge connections
                    )
            );
        });

        return new FixedLayoutFactory(positionsForFixedLayout, textNodesPositionsForFixedLayout, BasicForceLayout::new);
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

    private Set<String> getExpandedVoltageLevelIds(@NonNull Set<String> voltageLevelIds, Network network) {
        if (voltageLevelIds.isEmpty()) {
            return voltageLevelIds;
        }
        return VoltageLevelFilter.createVoltageLevelsDepthFilter(network, new ArrayList<>(voltageLevelIds), 1)
                        .getVoltageLevels().stream()
                        .map(VoltageLevel::getId)
                        .collect(Collectors.toSet());
    }

    private SvgAndMetadata drawSvgAndBuildMetadata(NadGenerationContext nadGenerationContext) {
        try (StringWriter svgWriter = new StringWriter(); StringWriter metadataWriter = new StringWriter()) {
            NetworkAreaDiagram.draw(
                    nadGenerationContext.getNetwork(),
                    svgWriter,
                    metadataWriter,
                    nadGenerationContext.getNadParameters(),
                    nadGenerationContext.getVoltageLevelFilter()
            );
            Map<String, Object> additionalMetadata = computeAdditionalMetadata(nadGenerationContext);

            return SvgAndMetadata.builder()
                    .svg(svgWriter.toString())
                    .metadata(metadataWriter.toString())
                    .additionalMetadata(additionalMetadata).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Updates the network with the substation's positions in an extension and return the coordinates for further processing.
     * Note : nadGenerationContext.network is modified by reference
     */
    public Map<String, Coordinate> assignGeoDataCoordinates(NadGenerationContext nadGenerationContext, List<Substation> substationsToFetch) {

        String substationsGeoDataString = geoDataService.getSubstationsGraphics(
                nadGenerationContext.getNetworkUuid(),
                nadGenerationContext.getVariantId(),
                substationsToFetch.stream().map(Substation::getId).toList()
        );
        List<SubstationGeoData> substationsGeoData = ResourceUtils.fromStringToSubstationGeoData(substationsGeoDataString, new ObjectMapper());
        Map<String, Coordinate> substationGeoDataMap = substationsGeoData.stream()
                .collect(Collectors.toMap(SubstationGeoData::getId, SubstationGeoData::getCoordinate));

        for (Substation substation : substationsToFetch) {
            if (nadGenerationContext.getNetwork().getSubstation(substation.getId()).getExtension(SubstationPosition.class) == null) {
                com.powsybl.sld.server.dto.Coordinate coordinate = substationGeoDataMap.get(substation.getId());
                if (coordinate != null) {
                    nadGenerationContext.getNetwork().getSubstation(substation.getId())
                            .newExtension(SubstationPositionAdder.class)
                            .withCoordinate(new com.powsybl.iidm.network.extensions.Coordinate(coordinate.getLat(), coordinate.getLon()))
                            .add();
                }
            }
        }
        return substationGeoDataMap;
    }

    private Map<String, Object> computeAdditionalMetadata(NadGenerationContext nadGenerationContext) {
        List<VoltageLevelInfos> voltageLevelsInfos = nadGenerationContext.getVoltageLevelFilter().getVoltageLevels().stream()
                .map(VoltageLevelInfos::new)
                .toList();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("nbVoltageLevels", nadGenerationContext.getVoltageLevelFilter().getNbVoltageLevels());
        metadata.put("voltageLevels", voltageLevelsInfos);
        metadata.put("scalingFactor", nadGenerationContext.getScalingFactor());

        return metadata;
    }

    @Transactional
    public void createNadPositionsConfigFromCsv(MultipartFile file) {
        if (!CsvFileValidator.hasCSVFormat(file)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid CSV format!");
        }

        List<NadVoltageLevelPositionInfos> positions;
        try {
            positions = getPositionsFromCsv(file);
            if (positions.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No positions found!");
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The csv is invalid!");
        }

        nadVoltageLevelConfiguredPositionRepository.deleteAll();
        nadVoltageLevelConfiguredPositionRepository.saveAll(positions.stream().map(NadVoltageLevelPositionInfos::toConfiguredPositionEntity).toList());
    }

    private List<NadVoltageLevelPositionInfos> parsePositions(CsvMapReader mapReader) throws IOException {
        String[] headers = CsvFileValidator.getHeaders(mapReader);
        if (headers.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The csv headers are invalid!");
        }
        List<NadVoltageLevelPositionInfos> nadVoltageLevelPositionInfos = new ArrayList<>(mapReader.getRowNumber());
        Map<String, String> row;
        while ((row = mapReader.read(headers)) != null) {
            String id = row.get(CsvFileValidator.VOLTAGE_LEVEL_ID);
            double xPosition = Double.parseDouble(row.get(CsvFileValidator.X_POSITION));
            double yPosition = Double.parseDouble(row.get(CsvFileValidator.Y_POSITION));
            double xLabelPosition = Double.parseDouble(row.get(CsvFileValidator.X_LABEL_POSITION));
            double yLabelPosition = Double.parseDouble(row.get(CsvFileValidator.Y_LABEL_POSITION));
            NadVoltageLevelPositionInfos positionInfos = NadVoltageLevelPositionInfos.builder()
                .voltageLevelId(id)
                .xPosition(xPosition)
                .yPosition(yPosition)
                .xLabelPosition(xLabelPosition)
                .yLabelPosition(yLabelPosition)
                .build();
            nadVoltageLevelPositionInfos.add(positionInfos);
        }
        return nadVoltageLevelPositionInfos;
    }

    private List<NadVoltageLevelPositionInfos> getPositionsFromCsv(MultipartFile file) throws IOException {
        try (BOMInputStream bomInputStream = BOMInputStream.builder().setInputStream(file.getInputStream()).setByteOrderMarks(ByteOrderMark.UTF_8).get();
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(bomInputStream, StandardCharsets.UTF_8));
            CsvMapReader mapReader = new CsvMapReader(fileReader, CsvFileValidator.CSV_PREFERENCE)) {
            return parsePositions(mapReader);
        }
    }
}
