package com.powsybl.single.line.diagram.server;

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
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    private Network getNetwork(String networkId) {
        try {
            return networkStoreService.getNetwork(networkId);
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT, "Network '" + networkId + "' not found");
        }
    }

    ResponseEntity<StreamingResponseBody> getVoltageLevelSvg(String networkId, String voltageLevelId) {
        Network network = getNetwork(networkId);

        VoltageLevelDiagram voltageLevelDiagram = createVoltageLevelDiagram(network, voltageLevelId);

        ByteArrayOutputStream svgByteArrayOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream metadataByteArrayOutputStream = new ByteArrayOutputStream();
        OutputStreamWriter svgWriter = new OutputStreamWriter(svgByteArrayOutputStream);
        OutputStreamWriter metadataWriter = new OutputStreamWriter(metadataByteArrayOutputStream);

        DefaultSVGWriter defaultSVGWriter = new DefaultSVGWriter(COMPONENT_LIBRARY, LAYOUT_PARAMETERS);
        DefaultDiagramInitialValueProvider defaultDiagramInitialValueProvider = new DefaultDiagramInitialValueProvider(network);
        DefaultDiagramStyleProvider defaultDiagramStyleProvider = new DefaultDiagramStyleProvider();
        DefaultNodeLabelConfiguration defaultNodeLabelConfiguration = new DefaultNodeLabelConfiguration(COMPONENT_LIBRARY);

        voltageLevelDiagram.writeSvg(
                "id",
                defaultSVGWriter,
                defaultDiagramInitialValueProvider,
                defaultDiagramStyleProvider,
                defaultNodeLabelConfiguration,
                svgWriter,
                metadataWriter);

        StreamingResponseBody stream = outputStream -> outputStream.write(svgByteArrayOutputStream.toByteArray());

        try {
            svgWriter.close();
            metadataWriter.close();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_XML).body(stream);
    }

    ResponseEntity<StreamingResponseBody> getVoltageLevelMetadata(String networkId, String voltageLevelId) {
        Network network = getNetwork(networkId);

        VoltageLevelDiagram voltageLevelDiagram = createVoltageLevelDiagram(network, voltageLevelId);

        ByteArrayOutputStream svgByteArrayOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream metadataByteArrayOutputStream = new ByteArrayOutputStream();
        OutputStreamWriter svgWriter = new OutputStreamWriter(svgByteArrayOutputStream);
        OutputStreamWriter metadataWriter = new OutputStreamWriter(metadataByteArrayOutputStream);

        DefaultSVGWriter defaultSVGWriter = new DefaultSVGWriter(COMPONENT_LIBRARY, LAYOUT_PARAMETERS);
        DefaultDiagramInitialValueProvider defaultDiagramInitialValueProvider = new DefaultDiagramInitialValueProvider(network);
        DefaultDiagramStyleProvider defaultDiagramStyleProvider = new DefaultDiagramStyleProvider();
        DefaultNodeLabelConfiguration defaultNodeLabelConfiguration = new DefaultNodeLabelConfiguration(COMPONENT_LIBRARY);

        voltageLevelDiagram.writeSvg(
                "id",
                defaultSVGWriter,
                defaultDiagramInitialValueProvider,
                defaultDiagramStyleProvider,
                defaultNodeLabelConfiguration,
                svgWriter,
                metadataWriter);

        StreamingResponseBody stream = outputStream -> outputStream.write(metadataByteArrayOutputStream.toByteArray());

        try {
            svgWriter.close();
            metadataWriter.close();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

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
                "id",
                defaultSVGWriter,
                defaultDiagramInitialValueProvider,
                defaultDiagramStyleProvider,
                defaultNodeLabelConfiguration,
                svgWriter,
                metadataWriter);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(byteArrayOutputStream);
        ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream);

        zipOutputStream.putNextEntry(new ZipEntry("svg"));
        IOUtils.copy(new StringReader(svgWriter.toString()), zipOutputStream);
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry("metadata"));
        IOUtils.copy(new StringReader(metadataWriter.toString()), zipOutputStream);
        zipOutputStream.closeEntry();

        zipOutputStream.finish();
        zipOutputStream.flush();
        zipOutputStream.close();

        bufferedOutputStream.close();
        byteArrayOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }
}
