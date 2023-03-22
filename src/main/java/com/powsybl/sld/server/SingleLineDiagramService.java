/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.layout.*;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.server.dto.SvgAndMetadata;
import com.powsybl.sld.server.utils.SldDisplayMode;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;
import com.powsybl.sld.util.TopologicalStyleProvider;
import com.powsybl.sld.server.utils.DiagramUtils;
import com.powsybl.sld.server.utils.SingleLineDiagramParameters;
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

import static com.powsybl.iidm.network.IdentifiableType.SUBSTATION;
import static com.powsybl.iidm.network.IdentifiableType.VOLTAGE_LEVEL;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
class SingleLineDiagramService {

    private static final LayoutParameters LAYOUT_PARAMETERS = new LayoutParameters()
            .setAdaptCellHeightToContent(true)
            .setHighlightLineState(true)
            .setCssLocation(LayoutParameters.CssLocation.EXTERNAL_NO_IMPORT);

    @Autowired
    private NetworkStoreService networkStoreService;

    public static Network getNetwork(UUID networkUuid, String variantId, NetworkStoreService networkStoreService) {
        return DiagramUtils.getNetwork(networkUuid, variantId, networkStoreService, null);
    }

    private static SubstationLayoutFactory getSubstationLayoutFactory(String substationLayout) {
        SubstationLayoutFactory substationLayoutFactory;
        switch (substationLayout) {
            case "horizontal":
                substationLayoutFactory = new HorizontalSubstationLayoutFactory();
                break;
            case "vertical":
                substationLayoutFactory = new VerticalSubstationLayoutFactory();
                break;
            default:
                throw new PowsyblException("Substation layout " + substationLayout + " incorrect");
        }

        return substationLayoutFactory;
    }

    SvgAndMetadata generateSvgAndMetadata(UUID networkUuid, String variantId, String id, SingleLineDiagramParameters diagParams) {
        Network network = getNetwork(networkUuid, variantId, networkStoreService);
        if (network.getVoltageLevel(id) == null && network.getSubstation(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voltage level or substation " + id + " not found");
        }

        try (var svgWriter = new StringWriter();
             var metadataWriter = new StringWriter()) {

            ComponentLibrary compLibrary = ComponentLibrary.find(diagParams.getComponentLibrary())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Component library '" + diagParams.getComponentLibrary() + "' not found"));

            var defaultDiagramStyleProvider = diagParams.isTopologicalColoring() ? new TopologicalStyleProvider(network)
                    : new NominalVoltageDiagramStyleProvider(network);
            DefaultDiagramLabelProvider labelProvider = null;
            LayoutParameters layoutParameters = new LayoutParameters(LAYOUT_PARAMETERS);
            layoutParameters.setLabelCentered(diagParams.isLabelCentered());
            layoutParameters.setLabelDiagonal(diagParams.isDiagonalLabel());
            layoutParameters.setUseName(diagParams.isUseName());
            layoutParameters.setLanguageTag(diagParams.getLanguage());

            if (diagParams.getSldDisplayMode() == SldDisplayMode.FEEDER_POSITION) {
                layoutParameters.setAddNodesInfos(false);
                layoutParameters.setLabelDiagonal(true);
                labelProvider = new PositionDiagramLabelProvider(network, compLibrary, layoutParameters, id);
            } else if (diagParams.getSldDisplayMode() == SldDisplayMode.STATE_VARIABLE) {
                layoutParameters.setAddNodesInfos(true);
                labelProvider = new DefaultDiagramLabelProvider(network, compLibrary, layoutParameters);
            } else {
                throw new PowsyblException(String.format("Given sld display mode %s doesn't exist", diagParams.getSldDisplayMode()));
            }

            var voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory(network);
            var substationLayoutFactory = getSubstationLayoutFactory(diagParams.getSubstationLayout());

            SingleLineDiagram.draw(network, id, svgWriter, metadataWriter, layoutParameters, compLibrary,
                    substationLayoutFactory, voltageLevelLayoutFactory, labelProvider, defaultDiagramStyleProvider, "");

            Map<String, Object> additionalMetadata = computeAdditionalMetadata(network, id);

            return SvgAndMetadata.builder()
                    .svg(svgWriter.toString())
                    .metadata(metadataWriter.toString())
                    .additionalMetadata(additionalMetadata).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, Object> computeAdditionalMetadata(Network network, String id) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", id);

        Identifiable<?> identifiable = network.getIdentifiable(id);
        if (identifiable.getType() == VOLTAGE_LEVEL) {
            VoltageLevel voltageLevel = network.getVoltageLevel(id);
            voltageLevel.getOptionalName().ifPresent(name -> metadata.put("name", name));
            voltageLevel.getSubstation().ifPresent(substation -> {
                metadata.put("substationId", substation.getId());
                substation.getCountry().ifPresent(country -> metadata.put("countryName", country.getName()));
            });
        } else if (identifiable.getType() == SUBSTATION) {
            Substation substation = network.getSubstation(id);
            substation.getOptionalName().ifPresent(name -> metadata.put("name", name));
            substation.getCountry().ifPresent(country -> metadata.put("countryName", country.getName()));
        } else {
            throw new PowsyblException("Given id '" + id + "' is not a substation or voltage level id in given network '" + network.getId() + "'");
        }

        return metadata;
    }

    Collection<String> getAvailableSvgComponentLibraries() {
        return ComponentLibrary.findAll().stream().map(ComponentLibrary::getName).collect(Collectors.toList());
    }
}
