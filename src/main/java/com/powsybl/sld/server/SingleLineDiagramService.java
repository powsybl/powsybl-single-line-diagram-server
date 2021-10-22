/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.sld.GraphBuilder;
import com.powsybl.sld.NetworkGraphBuilder;
import com.powsybl.sld.SubstationDiagram;
import com.powsybl.sld.VoltageLevelDiagram;
import com.powsybl.sld.layout.ForceSubstationLayoutFactory;
import com.powsybl.sld.layout.HorizontalSubstationLayoutFactory;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.layout.SmartVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.SubstationLayoutFactory;
import com.powsybl.sld.layout.VerticalSubstationLayoutFactory;
import com.powsybl.sld.layout.VoltageLevelLayoutFactory;
import com.powsybl.sld.library.ComponentLibrary;
import com.powsybl.sld.svg.DefaultDiagramLabelProvider;
import com.powsybl.sld.svg.DefaultDiagramStyleProvider;
import com.powsybl.sld.svg.DefaultSVGWriter;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;
import com.powsybl.sld.util.TopologicalStyleProvider;
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
import java.util.Optional;
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

    private Network getNetwork(UUID networkUuid) {
        try {
            return networkStoreService.getNetwork(networkUuid);
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Network '" + networkUuid + "' not found");
        }
    }

    // voltage level
    //
    private static VoltageLevelDiagram createVoltageLevelDiagram(Network network, String voltageLevelId, boolean useName) {
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        if (voltageLevel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voltage level " + voltageLevelId + " not found");
        }
        VoltageLevelLayoutFactory voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory(network);
        GraphBuilder graphBuilder = new NetworkGraphBuilder(network);
        return VoltageLevelDiagram.build(graphBuilder, voltageLevelId, voltageLevelLayoutFactory, useName);
    }

    Pair<String, String> generateSvgAndMetadata(UUID networkUuid, String voltageLevelId, boolean useName, boolean labelCentered,
                                                boolean diagonalLabel, boolean topologicalColoring, String componentLibrary) {
        Network network = getNetwork(networkUuid);

        VoltageLevelDiagram voltageLevelDiagram = createVoltageLevelDiagram(network, voltageLevelId, useName);

        try (StringWriter svgWriter = new StringWriter();
             StringWriter metadataWriter = new StringWriter()) {
            LayoutParameters renderedLayout = new LayoutParameters(LAYOUT_PARAMETERS);
            renderedLayout.setLabelCentered(labelCentered);
            renderedLayout.setLabelDiagonal(diagonalLabel);
            renderedLayout.setAddNodesInfos(true);

            Optional<ComponentLibrary> compLibrary = ComponentLibrary.find(componentLibrary);
            if (compLibrary.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Component library '" + componentLibrary + "' not found");
            }

            DefaultSVGWriter defaultSVGWriter = new DefaultSVGWriter(compLibrary.get(), renderedLayout);
            DefaultDiagramStyleProvider defaultDiagramStyleProvider = topologicalColoring ? new TopologicalStyleProvider(network)
                                                                                          : new NominalVoltageDiagramStyleProvider(network);
            DefaultDiagramLabelProvider labelProvider = new DefaultDiagramLabelProvider(network, compLibrary.get(), renderedLayout);

            voltageLevelDiagram.writeSvg("",
                                         defaultSVGWriter,
                                         labelProvider,
                                         defaultDiagramStyleProvider,
                                         svgWriter,
                                         metadataWriter);

            return Pair.of(svgWriter.toString(), metadataWriter.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // substation
    //
    private static SubstationDiagram createSubstationDiagram(Network network, String substationId, boolean useName,
                                                             String substationLayout) {
        Substation substation = network.getSubstation(substationId);
        if (substation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Substation " + substationId + " not found");
        }
        VoltageLevelLayoutFactory voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory(network);

        SubstationLayoutFactory substationLayoutFactory;
        switch (substationLayout) {
            case "horizontal" :
                substationLayoutFactory = new HorizontalSubstationLayoutFactory();
                break;
            case "vertical" :
                substationLayoutFactory = new VerticalSubstationLayoutFactory();
                break;
            case "smart" :
                substationLayoutFactory = new ForceSubstationLayoutFactory(ForceSubstationLayoutFactory.CompactionType.NONE);
                break;
            case "smartHorizontalCompaction" :
                substationLayoutFactory = new ForceSubstationLayoutFactory(ForceSubstationLayoutFactory.CompactionType.HORIZONTAL);
                break;
            case "smartVerticalCompaction" :
                substationLayoutFactory = new ForceSubstationLayoutFactory(ForceSubstationLayoutFactory.CompactionType.VERTICAL);
                break;
            default:
                throw new PowsyblException("Substation layout " + substationLayout + " incorrect");
        }

        GraphBuilder graphBuilder = new NetworkGraphBuilder(network);
        return SubstationDiagram.build(graphBuilder, substationId, substationLayoutFactory, voltageLevelLayoutFactory, useName);
    }

    Pair<String, String> generateSubstationSvgAndMetadata(UUID networkUuid, String substationId, boolean useName,
                                                          boolean labelCentered, boolean diagonalLabel, boolean topologicalColoring,
                                                          String substationLayout, String componentLibrary) {
        Network network = getNetwork(networkUuid);

        SubstationDiagram substationDiagram = createSubstationDiagram(network, substationId, useName, substationLayout);

        try (StringWriter svgWriter = new StringWriter();
             StringWriter metadataWriter = new StringWriter()) {
            LayoutParameters renderedLayout = new LayoutParameters(LAYOUT_PARAMETERS);
            renderedLayout.setLabelCentered(labelCentered);
            renderedLayout.setLabelDiagonal(diagonalLabel);
            renderedLayout.setAddNodesInfos(false);

            Optional<ComponentLibrary> compLibrary = ComponentLibrary.find(componentLibrary);
            if (compLibrary.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Component library '" + componentLibrary + "' not found");
            }

            DefaultSVGWriter defaultSVGWriter = new DefaultSVGWriter(compLibrary.get(), renderedLayout);
            DefaultDiagramStyleProvider defaultDiagramStyleProvider = topologicalColoring ? new TopologicalStyleProvider(network)
                    : new NominalVoltageDiagramStyleProvider(network);
            DefaultDiagramLabelProvider labelProvider = new DefaultDiagramLabelProvider(network, compLibrary.get(), renderedLayout);

            substationDiagram.writeSvg("",
                    defaultSVGWriter,
                    labelProvider,
                    defaultDiagramStyleProvider,
                    svgWriter,
                    metadataWriter);

            return Pair.of(svgWriter.toString(), metadataWriter.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    Collection<String> getAvailableSvgComponentLibraries() {
        return ComponentLibrary.findAll().stream().map(ComponentLibrary::getName).collect(Collectors.toList());
    }
}
