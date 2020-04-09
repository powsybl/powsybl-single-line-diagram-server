/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.sld.GraphBuilder;
import com.powsybl.sld.NetworkGraphBuilder;
import com.powsybl.sld.VoltageLevelDiagram;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.layout.SmartVoltageLevelLayoutFactory;
import com.powsybl.sld.layout.VoltageLevelLayoutFactory;
import com.powsybl.sld.library.ResourcesComponentLibrary;
import com.powsybl.sld.svg.DefaultDiagramInitialValueProvider;
import com.powsybl.sld.svg.DefaultDiagramStyleProvider;
import com.powsybl.sld.svg.DefaultNodeLabelConfiguration;
import com.powsybl.sld.svg.DefaultSVGWriter;
import com.powsybl.sld.util.NominalVoltageDiagramStyleProvider;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.util.UUID;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
class SingleLineDiagramService {

    private static final ResourcesComponentLibrary COMPONENT_LIBRARY = new ResourcesComponentLibrary("/ConvergenceLibrary");

    private static final LayoutParameters LAYOUT_PARAMETERS = new LayoutParameters()
            .setAdaptCellHeightToContent(true);

    @Autowired
    private NetworkStoreService networkStoreService;

    private Network getNetwork(UUID networkUuid) {
        try {
            return networkStoreService.getNetwork(networkUuid);
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Network '" + networkUuid + "' not found");
        }
    }

    private static VoltageLevelDiagram createVoltageLevelDiagram(Network network, String voltageLevelId, boolean useName) {
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        if (voltageLevel == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voltage level " + voltageLevelId + " not found");
        }
        VoltageLevelLayoutFactory voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory(network);
        GraphBuilder graphBuilder = new NetworkGraphBuilder(network);
        return VoltageLevelDiagram.build(graphBuilder, voltageLevelId, voltageLevelLayoutFactory, useName);
    }

    Pair<String, String> generateSvgAndMetadata(UUID networkUuid, String voltageLevelId, boolean useName, boolean labelCentered, boolean diagonalLabel) {
        Network network = getNetwork(networkUuid);

        VoltageLevelDiagram voltageLevelDiagram = createVoltageLevelDiagram(network, voltageLevelId, useName);

        try (StringWriter svgWriter = new StringWriter();
             StringWriter metadataWriter = new StringWriter()) {
            LayoutParameters renderedLayout = new LayoutParameters(LAYOUT_PARAMETERS);
            renderedLayout.setLabelCentered(labelCentered);
            renderedLayout.setLabelDiagonal(diagonalLabel);

            DefaultSVGWriter defaultSVGWriter = new DefaultSVGWriter(COMPONENT_LIBRARY, renderedLayout);
            DefaultDiagramInitialValueProvider defaultDiagramInitialValueProvider = new DefaultDiagramInitialValueProvider(network);
            DefaultDiagramStyleProvider defaultDiagramStyleProvider = new NominalVoltageDiagramStyleProvider();
            DefaultNodeLabelConfiguration defaultNodeLabelConfiguration = new DefaultNodeLabelConfiguration(COMPONENT_LIBRARY, renderedLayout);

            voltageLevelDiagram.writeSvg("",
                                         defaultSVGWriter,
                                         defaultDiagramInitialValueProvider,
                                         defaultDiagramStyleProvider,
                                         defaultNodeLabelConfiguration,
                                         svgWriter,
                                         metadataWriter);

            return Pair.of(svgWriter.toString(), metadataWriter.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
