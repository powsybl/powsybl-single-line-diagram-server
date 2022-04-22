/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.iidm.network.Network;
import com.powsybl.nad.NetworkAreaDiagram;
import com.powsybl.nad.svg.SvgParameters;
import com.powsybl.network.store.client.NetworkStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

/**
 * @author Etienne Homer<etienne.homer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
class NetworkAreaDiagramService {
    @Autowired
    private NetworkStoreService networkStoreService;

    public String generateNetworkAreaDiagramSvg(UUID networkUuid, String variantId, List<String> voltageLevelsIds, int depth) {
        Network network = SingleLineDiagramService.getNetwork(networkUuid, variantId, networkStoreService);
        voltageLevelsIds.forEach(voltageLevelId -> {
            if (network.getVoltageLevel(voltageLevelId) == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voltage level" + voltageLevelId + " not found");
            }
        });

        try (StringWriter svgWriter = new StringWriter()) {
            new NetworkAreaDiagram(network, voltageLevelsIds, depth).draw(svgWriter, new SvgParameters().setSvgWidthAndHeightAdded(true));

            return svgWriter.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
