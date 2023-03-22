/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.iidm.network.Network;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.layout.LayoutParameters;
import com.powsybl.nad.svg.StyleProvider;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.nad.svg.iidm.TopologicalStyleProvider;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.server.dto.SvgAndMetadata;
import com.powsybl.sld.server.utils.DiagramUtils;
import org.apache.commons.lang3.tuple.Pair;
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

/**
 * @author Etienne Homer<etienne.homer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
class NetworkAreaDiagramService {
    @Autowired
    private NetworkStoreService networkStoreService;

    public SvgAndMetadata generateNetworkAreaDiagramSvg(UUID networkUuid, String variantId, List<String> voltageLevelsIds, int depth) {
        Network network = DiagramUtils.getNetwork(networkUuid, variantId, networkStoreService, PreloadingStrategy.COLLECTION);
        voltageLevelsIds.forEach(voltageLevelId -> {
            if (network.getVoltageLevel(voltageLevelId) == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voltage level" + voltageLevelId + " not found");
            }
        });

        try (StringWriter svgWriter = new StringWriter()) {
            SvgParameters svgParameters = new SvgParameters()
                    .setSvgWidthAndHeightAdded(true)
                    .setCssLocation(SvgParameters.CssLocation.EXTERNAL_NO_IMPORT);
            LayoutParameters layoutParameters = new LayoutParameters();
            StyleProvider styleProvider = new TopologicalStyleProvider(network);
            new NetworkAreaDiagram(network, voltageLevelsIds, depth)
                    .draw(svgWriter, svgParameters, layoutParameters, styleProvider);

            Map additionalMetadata = computeAdditionalMetadata(network, voltageLevelsIds);

            return SvgAndMetadata.builder()
                    .svg(svgWriter.toString())
                    .additionalMetadata(additionalMetadata).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Map<String, Object> computeAdditionalMetadata(Network network, List<String> voltageLevelsIds) {

        // list of [id, name]
        List<Pair<String, String>> voltageLevelsInfos = voltageLevelsIds.stream()
                .map(network::getVoltageLevel)
                .map(voltageLevel -> Pair.of(voltageLevel.getId(), voltageLevel.getOptionalName().orElse(null)))
                .collect(Collectors.toList());

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("voltageLevels", voltageLevelsInfos);

        return metadata;
    }
}
