/**
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.iidm.network.*;
import com.powsybl.nad.model.ThreeWtEdge;
import com.powsybl.nad.svg.EdgeInfo;
import com.powsybl.nad.svg.SvgParameters;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
/**
 * @author SOUISSI Maissa <maissa.souissi@rte-france.com>
 */
class NadLabelProviderTest {

    @Test
    void testGetBranchEdgeInfo() {
        Network mockNetwork = mock(Network.class);
        Branch<?> mockLine = mock(Line.class);
        Terminal terminal1 = mock(Terminal.class);
        Terminal terminal2 = mock(Terminal.class);
        CurrentLimits limits1 = mock(CurrentLimits.class);
        CurrentLimits limits2 = mock(CurrentLimits.class);

        when(mockNetwork.getBranch("LINE1")).thenReturn(mockLine);
        when(mockLine.getTerminal(TwoSides.ONE)).thenReturn(terminal1);
        when(mockLine.getTerminal(TwoSides.TWO)).thenReturn(terminal2);

        when(terminal1.getP()).thenReturn(50.0);
        when(terminal2.getP()).thenReturn(-30.0);
        when(terminal1.getI()).thenReturn(1.0);
        when(terminal2.getI()).thenReturn(2.0);
        when(mockLine.getCurrentLimits(TwoSides.ONE)).thenReturn(Optional.of(limits1));
        when(mockLine.getCurrentLimits(TwoSides.TWO)).thenReturn(Optional.of(limits2));
        when(limits1.getPermanentLimit()).thenReturn(2.0);
        when(limits2.getPermanentLimit()).thenReturn(4.0);

        SvgParameters svgParameters = mock(SvgParameters.class);
        when(svgParameters.createValueFormatter()).thenReturn(new SvgParameters().createValueFormatter());

        NadLabelProvider provider = new NadLabelProvider(mockNetwork, svgParameters);

        Optional<EdgeInfo> edgeInfoOpt = provider.getBranchEdgeInfo("LINE1", "LINE");
        assertTrue(edgeInfoOpt.isPresent());
        EdgeInfo edgeInfo = edgeInfoOpt.get();

        assertEquals("50", edgeInfo.getLabelA().orElse(""));
        assertEquals("50 %", edgeInfo.getLabelB().orElse(""));
    }

    @Test
    void testGetThreeWindingTransformerEdgeInfo() {
        Network mockNetwork = mock(Network.class);
        ThreeWindingsTransformer twt = mock(ThreeWindingsTransformer.class);

        Terminal term1 = mock(Terminal.class);
        Terminal term2 = mock(Terminal.class);
        Terminal term3 = mock(Terminal.class);

        CurrentLimits limits1 = mock(CurrentLimits.class);
        CurrentLimits limits2 = mock(CurrentLimits.class);
        CurrentLimits limits3 = mock(CurrentLimits.class);

        when(mockNetwork.getThreeWindingsTransformer("TWT1")).thenReturn(twt);
        when(twt.getTerminal(ThreeSides.ONE)).thenReturn(term1);
        when(twt.getTerminal(ThreeSides.TWO)).thenReturn(term2);
        when(twt.getTerminal(ThreeSides.THREE)).thenReturn(term3);

        when(twt.getLeg(ThreeSides.ONE)).thenReturn(mock(ThreeWindingsTransformer.Leg.class));
        when(twt.getLeg(ThreeSides.TWO)).thenReturn(mock(ThreeWindingsTransformer.Leg.class));
        when(twt.getLeg(ThreeSides.THREE)).thenReturn(mock(ThreeWindingsTransformer.Leg.class));

        when(term1.getP()).thenReturn(20.0);
        when(term2.getP()).thenReturn(-40.0);
        when(term3.getP()).thenReturn(10.0);

        when(term1.getI()).thenReturn(1.0);
        when(term2.getI()).thenReturn(2.0);
        when(term3.getI()).thenReturn(3.0);

        when(twt.getLeg(ThreeSides.ONE).getCurrentLimits()).thenReturn(Optional.of(limits1));
        when(twt.getLeg(ThreeSides.TWO).getCurrentLimits()).thenReturn(Optional.of(limits2));
        when(twt.getLeg(ThreeSides.THREE).getCurrentLimits()).thenReturn(Optional.of(limits3));

        when(limits1.getPermanentLimit()).thenReturn(2.0);
        when(limits2.getPermanentLimit()).thenReturn(4.0);
        when(limits3.getPermanentLimit()).thenReturn(6.0);

        SvgParameters svgParameters = mock(SvgParameters.class);
        when(svgParameters.createValueFormatter()).thenReturn(new SvgParameters().createValueFormatter());

        NadLabelProvider provider = new NadLabelProvider(mockNetwork, svgParameters);

        Optional<EdgeInfo> edgeInfoOpt = provider.getThreeWindingTransformerEdgeInfo("TWT1", ThreeWtEdge.Side.ONE);
        assertTrue(edgeInfoOpt.isPresent());
        EdgeInfo edgeInfo = edgeInfoOpt.get();

        assertEquals("40", edgeInfo.getLabelA().orElse(""));
        assertEquals("50 %", edgeInfo.getLabelB().orElse(""));
    }

    @Test
    void testBranchEdgeInfoWhenBranchNull() {
        Network mockNetwork = mock(Network.class);
        SvgParameters svgParameters = mock(SvgParameters.class);
        when(svgParameters.createValueFormatter()).thenReturn(new SvgParameters().createValueFormatter());

        NadLabelProvider provider = new NadLabelProvider(mockNetwork, svgParameters);
        Optional<EdgeInfo> edgeInfoOpt = provider.getBranchEdgeInfo("NON_EXISTENT", "LINE");
        assertTrue(edgeInfoOpt.isEmpty());
    }

    @Test
    void testThreeWindingEdgeInfoWhenTwtNull() {
        Network mockNetwork = mock(Network.class);
        SvgParameters svgParameters = mock(SvgParameters.class);
        when(svgParameters.createValueFormatter()).thenReturn(new SvgParameters().createValueFormatter());

        NadLabelProvider provider = new NadLabelProvider(mockNetwork, svgParameters);
        Optional<EdgeInfo> edgeInfoOpt = provider.getThreeWindingTransformerEdgeInfo("NON_EXISTENT", ThreeWtEdge.Side.ONE);
        assertTrue(edgeInfoOpt.isEmpty());
    }
}
