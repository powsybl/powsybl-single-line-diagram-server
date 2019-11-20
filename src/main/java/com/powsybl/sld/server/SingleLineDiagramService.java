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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.powsybl.sld.server.SingleLineDiagramApi.DiagramRequest;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
class SingleLineDiagramService {

    private static final ResourcesComponentLibrary COMPONENT_LIBRARY = new ResourcesComponentLibrary("/ConvergenceLibrary");

    private static final LayoutParameters LAYOUT_PARAMETERS = new LayoutParameters();

    @Autowired
    private NetworkStoreService networkStoreService;

    private static VoltageLevelDiagram createVoltageLevelDiagram(Network network, String voltageLevelId) {
        VoltageLevel voltageLevel = network.getVoltageLevel(voltageLevelId);
        if (voltageLevel == null) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Voltage level " + voltageLevelId + " not found");
        }
        VoltageLevelLayoutFactory voltageLevelLayoutFactory = new SmartVoltageLevelLayoutFactory(network);
        GraphBuilder graphBuilder = new NetworkGraphBuilder(network);
        return VoltageLevelDiagram.build(graphBuilder, voltageLevelId, voltageLevelLayoutFactory, false, false);
    }

    private StreamingResponseBody generateDiagramStream(String networkId, String voltageLevelId, DiagramRequest diagramRequested) {
        Network network = getNetwork(networkId);

        VoltageLevelDiagram voltageLevelDiagram = createVoltageLevelDiagram(network, voltageLevelId);

        try (ByteArrayOutputStream svgByteArrayOutputStream = new ByteArrayOutputStream();
             ByteArrayOutputStream metadataByteArrayOutputStream = new ByteArrayOutputStream();
             OutputStreamWriter svgWriter = new OutputStreamWriter(svgByteArrayOutputStream);
             OutputStreamWriter metadataWriter = new OutputStreamWriter(metadataByteArrayOutputStream)) {

            DefaultSVGWriter defaultSVGWriter = new DefaultSVGWriter(COMPONENT_LIBRARY, LAYOUT_PARAMETERS);
            DefaultDiagramInitialValueProvider defaultDiagramInitialValueProvider = new DefaultDiagramInitialValueProvider(network);
            DefaultDiagramStyleProvider defaultDiagramStyleProvider = new DefaultDiagramStyleProvider();
            DefaultNodeLabelConfiguration defaultNodeLabelConfiguration = new DefaultNodeLabelConfiguration(COMPONENT_LIBRARY);

            voltageLevelDiagram.writeSvg(
                    "",
                    defaultSVGWriter,
                    defaultDiagramInitialValueProvider,
                    defaultDiagramStyleProvider,
                    defaultNodeLabelConfiguration,
                    svgWriter,
                    metadataWriter);

            switch (diagramRequested) {
                case SVG:
                    return outputStream -> outputStream.write(svgByteArrayOutputStream.toByteArray());

                case METADATA:
                    return outputStream -> outputStream.write(metadataByteArrayOutputStream.toByteArray());

                default:
                    throw new IllegalStateException("Unknown diagram requested value: " + diagramRequested);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Network getNetwork(String networkId) {
        try {
            return networkStoreService.getNetwork(networkId);
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Network '" + networkId + "' not found");
        }
    }

    ResponseEntity<StreamingResponseBody> getVoltageLevelSvg(String networkId, String voltageLevelId) {
        StreamingResponseBody stream = generateDiagramStream(networkId, voltageLevelId, DiagramRequest.SVG);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(stream);
    }

    ResponseEntity<StreamingResponseBody> getVoltageLevelMetadata(String networkId, String voltageLevelId) {
        StreamingResponseBody stream = generateDiagramStream(networkId, voltageLevelId, DiagramRequest.METADATA);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(stream);
    }

    byte[] getVoltageLevelCompleteSvg(String networkId, String voltageLevelId) throws IOException {
        Network network = getNetwork(networkId);

        VoltageLevelDiagram voltageLevelDiagram = createVoltageLevelDiagram(network, voltageLevelId);

        StringWriter svgWriter = new StringWriter();
        StringWriter metadataWriter = new StringWriter();

        DefaultSVGWriter defaultSVGWriter = new DefaultSVGWriter(COMPONENT_LIBRARY, LAYOUT_PARAMETERS);
        DefaultDiagramInitialValueProvider defaultDiagramInitialValueProvider = new DefaultDiagramInitialValueProvider(network);
        DefaultDiagramStyleProvider defaultDiagramStyleProvider = new DefaultDiagramStyleProvider();
        DefaultNodeLabelConfiguration defaultNodeLabelConfiguration = new DefaultNodeLabelConfiguration(COMPONENT_LIBRARY);

        voltageLevelDiagram.writeSvg(
                "",
                defaultSVGWriter,
                defaultDiagramInitialValueProvider,
                defaultDiagramStyleProvider,
                defaultNodeLabelConfiguration,
                svgWriter,
                metadataWriter);

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(byteArrayOutputStream))) {

            zipOutputStream.putNextEntry(new ZipEntry("svg"));
            zipOutputStream.write(svgWriter.toString().getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.putNextEntry(new ZipEntry("metadata"));
            zipOutputStream.write(metadataWriter.toString().getBytes(StandardCharsets.UTF_8));
            zipOutputStream.closeEntry();

            zipOutputStream.finish();
            zipOutputStream.flush();

            return byteArrayOutputStream.toByteArray();
        }
    }
}
