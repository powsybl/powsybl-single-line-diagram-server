/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.diagram.components.ComponentSize;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.extensions.OperatingStatus;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.model.coordinate.Direction;
import com.powsybl.sld.model.nodes.*;
import com.powsybl.sld.svg.LabelProvider;
import com.powsybl.sld.svg.SvgParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@SpringBootTest
class CommonLabelProviderTest {

    @MockitoBean
    private NetworkStoreService networkStoreService;

    @Autowired
    private SingleLineDiagramService singleLineDiagramService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testGetNodeDecoratorsWithEmbeddedMiddle3WTNode() {
        Network mockNetwork = mock(Network.class);
        ThreeWindingsTransformer mockTransformer = mock(ThreeWindingsTransformer.class);
        when(mockNetwork.getThreeWindingsTransformer(anyString())).thenReturn(mockTransformer);

        var leg1 = mock(ThreeWindingsTransformer.Leg.class);
        var leg2 = mock(ThreeWindingsTransformer.Leg.class);
        var leg3 = mock(ThreeWindingsTransformer.Leg.class);
        when(mockTransformer.getLeg1()).thenReturn(leg1);
        when(mockTransformer.getLeg2()).thenReturn(leg2);
        when(mockTransformer.getLeg3()).thenReturn(leg3);
        when(leg1.getTerminal()).thenReturn(mock(Terminal.class));
        when(leg2.getTerminal()).thenReturn(mock(Terminal.class));
        when(leg3.getTerminal()).thenReturn(mock(Terminal.class));
        when(leg1.getTerminal().isConnected()).thenReturn(false);
        when(leg2.getTerminal().isConnected()).thenReturn(false);
        when(leg3.getTerminal().isConnected()).thenReturn(false);

        Middle3WTNode middle3WTNode = mock(Middle3WTNode.class);
        when(middle3WTNode.isEmbeddedInVlGraph()).thenReturn(true);
        when(middle3WTNode.getEquipmentId()).thenReturn("3WT1");

        OperatingStatus<ThreeWindingsTransformer> mockStatus = mock(OperatingStatus.class);
        when(mockStatus.getStatus()).thenReturn(OperatingStatus.Status.FORCED_OUTAGE);
        when(mockTransformer.getExtension(OperatingStatus.class)).thenReturn(mockStatus);

        LayoutParameters layoutParameters = mock(LayoutParameters.class);
        SvgParameters svgParameters = mock(SvgParameters.class);
        SldComponentLibrary mockLibrary = mock(SldComponentLibrary.class);
        when(mockLibrary.getSize(any())).thenReturn(new ComponentSize(40, 20));
        CommonLabelProvider provider = new CommonLabelProvider(mockNetwork, mockLibrary, layoutParameters, svgParameters);
        List<LabelProvider.NodeDecorator> decorators = provider.getNodeDecorators(middle3WTNode, Direction.TOP);
        assertTrue(decorators.stream().anyMatch(d -> d.getType().equals("FLASH")));
    }

    @Test
    void testGetNodeDecoratorsWithFeederNode() {
        Network mockNetwork = mock(Network.class);
        Line mockLine = mock(Line.class);
        when(mockNetwork.getBranch(eq("LINE_S2S3"))).thenReturn(mockLine);

        FeederNode feederNode = mock(FeederNode.class);
        Feeder feeder = mock(Feeder.class);
        when(feederNode.getFeeder()).thenReturn(feeder);
        when(feeder.getFeederType()).thenReturn(FeederType.BRANCH);
        when(feederNode.getEquipmentId()).thenReturn("LINE_S2S3");

        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        when(mockLine.getTerminal1()).thenReturn(terminal1);
        when(mockLine.getTerminal2()).thenReturn(terminal2);
        when(terminal1.isConnected()).thenReturn(false);
        when(terminal2.isConnected()).thenReturn(false);

        OperatingStatus<Line> mockStatus = mock(OperatingStatus.class);
        when(mockStatus.getStatus()).thenReturn(OperatingStatus.Status.FORCED_OUTAGE);
        when(mockLine.getExtension(OperatingStatus.class)).thenReturn(mockStatus);

        LayoutParameters layoutParameters = mock(LayoutParameters.class);
        SvgParameters svgParameters = mock(SvgParameters.class);
        SldComponentLibrary mockLibrary = mock(SldComponentLibrary.class);
        when(mockLibrary.getSize(any())).thenReturn(new ComponentSize(40, 20));

        CommonLabelProvider provider = new CommonLabelProvider(mockNetwork, mockLibrary, layoutParameters, svgParameters);
        List<LabelProvider.NodeDecorator> decorators = provider.getNodeDecorators(feederNode, Direction.TOP);
        assertTrue(decorators.stream().anyMatch(d -> d.getType().equals("FLASH")));
    }

    @Test
    void testGetNodeDecoratorsWithBusNode() {
        Network mockNetwork = mock(Network.class);
        BusbarSection mockBusbarSection = mock(BusbarSection.class);

        when(mockNetwork.getBusbarSection(eq("S1VL1_BBS"))).thenReturn(mockBusbarSection);
        when(mockNetwork.getIdentifiable(eq("S1VL1_BBS"))).thenReturn((Identifiable) mockBusbarSection);
        BusNode busNode = mock(BusNode.class);
        when(busNode.getEquipmentId()).thenReturn("S1VL1_BBS");

        Terminal mockTerminal = mock(Terminal.class);
        when(mockTerminal.isConnected()).thenReturn(false);
        when(mockBusbarSection.getTerminal()).thenReturn(mockTerminal);
        when(mockBusbarSection.getTerminals()).thenReturn((List) List.of(mockTerminal));
        OperatingStatus<BusbarSection> mockStatus = mock(OperatingStatus.class);
        when(mockStatus.getStatus()).thenReturn(OperatingStatus.Status.PLANNED_OUTAGE);
        when(mockBusbarSection.getExtension(OperatingStatus.class)).thenReturn(mockStatus);

        LayoutParameters layoutParameters = mock(LayoutParameters.class);
        SvgParameters svgParameters = mock(SvgParameters.class);
        SldComponentLibrary mockLibrary = mock(SldComponentLibrary.class);
        when(mockLibrary.getSize(any())).thenReturn(new ComponentSize(40, 20));

        CommonLabelProvider provider = new CommonLabelProvider(mockNetwork, mockLibrary, layoutParameters, svgParameters);
        List<LabelProvider.NodeDecorator> decorators = provider.getNodeDecorators(busNode, Direction.TOP);
        assertTrue(decorators.stream().anyMatch(d -> d.getType().equals("LOCK")));
    }

    @Test
    void testGetNodeDecoratorsWithHvdcFeederNode() {
        Network mockNetwork = mock(Network.class);
        HvdcLine mockHvdcLine = mock(HvdcLine.class);
        HvdcConverterStation mockConverterStation = mock(HvdcConverterStation.class);
        FeederNode feederNode = mock(FeederNode.class);
        Feeder feeder = mock(Feeder.class);
        when(feederNode.getFeeder()).thenReturn(feeder);
        when(feeder.getFeederType()).thenReturn(FeederType.HVDC);
        when(feederNode.getEquipmentId()).thenReturn("VSC1");
        when(mockNetwork.getHvdcLine(eq("VSC1"))).thenReturn(mockHvdcLine);
        when(mockNetwork.getHvdcConverterStation(eq("VSC1"))).thenReturn(mockConverterStation);
        when(mockConverterStation.getHvdcLine()).thenReturn(mockHvdcLine);
        HvdcConverterStation converter1 = mock(HvdcConverterStation.class);
        HvdcConverterStation converter2 = mock(HvdcConverterStation.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        when(converter1.getTerminal()).thenReturn(terminal1);
        when(converter2.getTerminal()).thenReturn(terminal2);
        when(terminal1.isConnected()).thenReturn(false);
        when(terminal2.isConnected()).thenReturn(false);
        when(mockHvdcLine.getConverterStation1()).thenReturn(converter1);
        when(mockHvdcLine.getConverterStation2()).thenReturn(converter2);
        OperatingStatus<HvdcLine> mockStatus = mock(OperatingStatus.class);
        when(mockStatus.getStatus()).thenReturn(OperatingStatus.Status.FORCED_OUTAGE);
        when(mockHvdcLine.getExtension(OperatingStatus.class)).thenReturn(mockStatus);
        when(mockNetwork.getIdentifiable(eq("VSC1"))).thenReturn(mockConverterStation);
        LayoutParameters layoutParameters = mock(LayoutParameters.class);
        SvgParameters svgParameters = mock(SvgParameters.class);
        SldComponentLibrary mockLibrary = mock(SldComponentLibrary.class);
        when(mockLibrary.getSize(any())).thenReturn(new ComponentSize(40, 20));
        CommonLabelProvider provider = new CommonLabelProvider(mockNetwork, mockLibrary, layoutParameters, svgParameters);
        List<LabelProvider.NodeDecorator> decorators = provider.getNodeDecorators(feederNode, Direction.TOP);
        assertTrue(decorators.stream().anyMatch(d -> d.getType().equals("FLASH")));
    }
}
