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
import com.powsybl.sld.SldParameters;
import com.powsybl.sld.layout.*;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.server.dto.CurrentLimitViolationInfos;
import com.powsybl.sld.server.dto.EquipmentInfos;
import com.powsybl.sld.server.dto.SubstationInfos;
import com.powsybl.sld.server.dto.SvgAndMetadata;
import com.powsybl.sld.server.dto.VoltageLevelInfos;
import com.powsybl.sld.server.utils.*;
import com.powsybl.sld.svg.SvgParameters;
import com.powsybl.sld.svg.styles.NominalVoltageStyleProvider;
import com.powsybl.sld.svg.styles.StyleProvidersList;
import com.powsybl.sld.svg.styles.iidm.HighlightLineStateStyleProvider;
import com.powsybl.sld.svg.styles.iidm.TopologicalStyleProvider;
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
import static com.powsybl.sld.svg.styles.StyleClassConstants.OVERLOAD_STYLE_CLASS;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
class SingleLineDiagramService {

    private static final LayoutParameters LAYOUT_PARAMETERS = new LayoutParameters()
            .setAdaptCellHeightToContent(true);

    private static final SvgParameters SVG_PARAMETERS = new SvgParameters()
            .setCssLocation(SvgParameters.CssLocation.EXTERNAL_NO_IMPORT);

    @Autowired
    private NetworkStoreService networkStoreService;

    public static Network getNetwork(UUID networkUuid, String variantId, NetworkStoreService networkStoreService) {
        return DiagramUtils.getNetwork(networkUuid, variantId, networkStoreService, null);
    }

    private static SubstationLayoutFactory getSubstationLayoutFactory(String substationLayout) {
        return switch (substationLayout) {
            case "horizontal" -> new HorizontalSubstationLayoutFactory();
            case "vertical" -> new VerticalSubstationLayoutFactory();
            default -> throw new PowsyblException("Substation layout " + substationLayout + " incorrect");
        };
    }

    SvgAndMetadata generateSvgAndMetadata(UUID networkUuid, String variantId, String id, SingleLineDiagramParameters diagParams, List<CurrentLimitViolationInfos> currentLimitViolationInfos) {
        Network network = getNetwork(networkUuid, variantId, networkStoreService);
        if (network.getVoltageLevel(id) == null && network.getSubstation(id) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voltage level or substation " + id + " not found");
        }

        try (var svgWriter = new StringWriter();
             var metadataWriter = new StringWriter()) {

            SldComponentLibrary compLibrary = SldComponentLibrary.find(diagParams.getComponentLibrary())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Component library '" + diagParams.getComponentLibrary() + "' not found"));

            SvgParameters svgParameters = new SvgParameters(SVG_PARAMETERS);
            svgParameters.setLabelCentered(diagParams.isLabelCentered());
            svgParameters.setLabelDiagonal(diagParams.isDiagonalLabel());
            svgParameters.setUseName(diagParams.isUseName());
            svgParameters.setUndefinedValueSymbol("\u2014");
            svgParameters.setLanguageTag(diagParams.getLanguage());
            svgParameters.setUnifyVoltageLevelColors(true);
            svgParameters.setDisplayCurrentFeederInfo(true);
            svgParameters.setDisplayPermanentLimitPercentageFeederInfo(true);
            LayoutParameters layoutParameters = new LayoutParameters(LAYOUT_PARAMETERS);

            SldParameters sldParameters = new SldParameters();

            if (diagParams.getSldDisplayMode() == SldDisplayMode.FEEDER_POSITION) {
                svgParameters.setBusesLegendAdded(false);
                svgParameters.setLabelDiagonal(true);
                sldParameters.setLabelProviderFactory(PositionDiagramLabelProvider.newLabelProviderFactory(id));
            } else if (diagParams.getSldDisplayMode() == SldDisplayMode.STATE_VARIABLE) {
                svgParameters.setBusesLegendAdded(true);
                sldParameters.setLabelProviderFactory(CommonLabelProvider::new);
            } else {
                throw new PowsyblException(String.format("Given sld display mode %s doesn't exist", diagParams.getSldDisplayMode()));
            }

            var voltageLevelLayoutFactory = CustomVoltageLevelLayoutFactoryCreator.newCustomVoltageLevelLayoutFactoryCreator();
            var substationLayoutFactory = getSubstationLayoutFactory(diagParams.getSubstationLayout());

            sldParameters.setSvgParameters(svgParameters);
            sldParameters.setSubstationLayoutFactory(substationLayoutFactory);
            sldParameters.setVoltageLevelLayoutFactoryCreator(voltageLevelLayoutFactory);
            sldParameters.setLayoutParameters(layoutParameters);

            Map<String, String> limitViolationStyles = DiagramUtils.createLimitViolationStyles(currentLimitViolationInfos, OVERLOAD_STYLE_CLASS);

            sldParameters.setStyleProviderFactory((net, parameters) -> {
                return diagParams.isTopologicalColoring()
                    ? new StyleProvidersList(new TopologicalStyleProvider(network, parameters),
                                             new HighlightLineStateStyleProvider(network),
                                             new LimitHighlightStyleProvider(network, limitViolationStyles))
                    : new StyleProvidersList(new NominalVoltageStyleProvider(),
                                             new HighlightLineStateStyleProvider(network),
                                             new LimitHighlightStyleProvider(network, limitViolationStyles));
            });
            sldParameters.setComponentLibrary(compLibrary);

            SingleLineDiagram.draw(network, id, svgWriter, metadataWriter, sldParameters);

            EquipmentInfos additionalMetadata = computeAdditionalMetadata(network, id);

            return SvgAndMetadata.builder()
                    .svg(svgWriter.toString())
                    .metadata(metadataWriter.toString())
                    .additionalMetadata(additionalMetadata).build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private EquipmentInfos computeAdditionalMetadata(Network network, String id) {

        Identifiable<?> identifiable = network.getIdentifiable(id);
        if (identifiable.getType() == VOLTAGE_LEVEL) {
            VoltageLevel voltageLevel = network.getVoltageLevel(id);
            return new VoltageLevelInfos(voltageLevel);
        } else if (identifiable.getType() == SUBSTATION) {
            Substation substation = network.getSubstation(id);
            return new SubstationInfos(substation);
        } else {
            throw new PowsyblException("Given id '" + id + "' is not a substation or voltage level id in given network '" + network.getId() + "'");
        }
    }

    Collection<String> getAvailableSvgComponentLibraries() {
        return SldComponentLibrary.findAll().stream().map(SldComponentLibrary::getName).collect(Collectors.toList());
    }
}
