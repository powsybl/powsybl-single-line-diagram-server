/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.layout.*;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;
import com.powsybl.sld.util.TopologicalStyleProvider;
import com.powsybl.sld.utils.DiagramParameters;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

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

    private Network getNetwork(UUID networkUuid, String variantId) {
        try {
            Network network = networkStoreService.getNetwork(networkUuid);
            if (variantId != null) {
                network.getVariantManager().setWorkingVariant(variantId);
            }
            return network;
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
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
            case "smart":
                substationLayoutFactory = new ForceSubstationLayoutFactory(ForceSubstationLayoutFactory.CompactionType.NONE);
                break;
            case "smartHorizontalCompaction":
                substationLayoutFactory = new ForceSubstationLayoutFactory(ForceSubstationLayoutFactory.CompactionType.HORIZONTAL);
                break;
            case "smartVerticalCompaction":
                substationLayoutFactory = new ForceSubstationLayoutFactory(ForceSubstationLayoutFactory.CompactionType.VERTICAL);
                break;
            default:
                throw new PowsyblException("Substation layout " + substationLayout + " incorrect");
        }

        return substationLayoutFactory;
    }

    Pair<String, String> generateSvgAndMetadata(UUID networkUuid, String variantId, String id, DiagramParameters diagParams) {
        Network network = getNetwork(networkUuid, variantId);
        // FIXME: will be unnecessary in next release because, check will be done inside SLD library
        if (network.getVoltageLevel(id) == null && network.getSubstation(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voltage level or substation " + id + " not found");
        }

        try (StringWriter svgWriter = new StringWriter();
             StringWriter metadataWriter = new StringWriter()) {
            LayoutParameters layoutParameters = new LayoutParameters(LAYOUT_PARAMETERS);
            layoutParameters.setLabelCentered(diagParams.isLabelCentered());
            layoutParameters.setLabelDiagonal(diagParams.isDiagonalLabel());
            layoutParameters.setUseName(diagParams.isUseName());
            layoutParameters.setAddNodesInfos(true); // only used for voltage level diagrams

            ComponentLibrary compLibrary = ComponentLibrary.find(diagParams.getComponentLibrary())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Component library '" + diagParams.getComponentLibrary() + "' not found"));

            var defaultDiagramStyleProvider = diagParams.isTopologicalColoring() ? new TopologicalStyleProvider(network)
                                                                                 : new NominalVoltageDiagramStyleProvider(network);
            var labelProvider = new DefaultDiagramLabelProvider(network, compLibrary, layoutParameters);

            var voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory(network);
            var substationLayoutFactory = getSubstationLayoutFactory(diagParams.getSubstationLayout());
            SingleLineDiagram.draw(network, id, svgWriter, metadataWriter, layoutParameters, compLibrary,
                    substationLayoutFactory, voltageLevelLayoutFactory, labelProvider, defaultDiagramStyleProvider, "");

            return Pair.of(svgWriter.toString(), metadataWriter.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Collection<String> getAvailableSvgComponentLibraries() {
        return ComponentLibrary.findAll().stream().map(ComponentLibrary::getName).collect(Collectors.toList());
    }
}
