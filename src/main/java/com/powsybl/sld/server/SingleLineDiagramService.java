/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import com.powsybl.commons.config.BaseVoltagesConfig;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.sld.SingleLineDiagram;
import com.powsybl.sld.SldParameters;
import com.powsybl.sld.layout.HorizontalSubstationLayoutFactory;
import com.powsybl.sld.layout.LayoutParameters;
import com.powsybl.sld.layout.SubstationLayoutFactory;
import com.powsybl.sld.layout.VerticalSubstationLayoutFactory;
import com.powsybl.sld.library.SldComponentLibrary;
import com.powsybl.sld.server.dto.*;
import com.powsybl.sld.server.error.SingleLineDiagramBusinessException;
import com.powsybl.sld.server.error.SingleLineDiagramRuntimeException;
import com.powsybl.sld.server.utils.*;
import com.powsybl.sld.svg.SvgParameters;
import com.powsybl.sld.svg.styles.NominalVoltageStyleProvider;
import com.powsybl.sld.svg.styles.StyleProvidersList;
import com.powsybl.sld.svg.styles.iidm.HighlightLineStateStyleProvider;
import com.powsybl.sld.svg.styles.iidm.TopologicalStyleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.powsybl.iidm.network.IdentifiableType.SUBSTATION;
import static com.powsybl.iidm.network.IdentifiableType.VOLTAGE_LEVEL;
import static com.powsybl.sld.server.error.SingleLineDiagramBusinessErrorCode.EQUIPMENT_NOT_FOUND;
import static com.powsybl.sld.server.error.SingleLineDiagramBusinessErrorCode.INVALID_CONFIG_REQUEST;
import static com.powsybl.sld.server.error.SingleLineDiagramBusinessErrorCode.INVALID_EQUIPMENT;
import static com.powsybl.sld.server.error.SingleLineDiagramBusinessErrorCode.INVALID_SUBSTATION_LAYOUT;
import static com.powsybl.sld.svg.styles.StyleClassConstants.OVERLOAD_STYLE_CLASS;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
@ComponentScan(basePackageClasses = {NetworkStoreService.class})
@Service
class SingleLineDiagramService {
    private static final double DEFAULT_LAYOUT_PADDING = 20;
    private static final double LAYOUT_BOTTOM_PADDING = 100;

    private static final LayoutParameters LAYOUT_PARAMETERS = new LayoutParameters()
            .setAdaptCellHeightToContent(true)
            .setDiagrammPadding(DEFAULT_LAYOUT_PADDING, DEFAULT_LAYOUT_PADDING, DEFAULT_LAYOUT_PADDING, LAYOUT_BOTTOM_PADDING);

    private static final SvgParameters SVG_PARAMETERS = new SvgParameters()
            .setCssLocation(SvgParameters.CssLocation.EXTERNAL_NO_IMPORT);

    @Autowired
    private NetworkStoreService networkStoreService;

    public static Network getNetwork(UUID networkUuid, String variantId, NetworkStoreService networkStoreService) {
        return DiagramUtils.getNetwork(networkUuid, variantId, networkStoreService, null);
    }

    private static SubstationLayoutFactory getSubstationLayoutFactory(String substationLayout) {
        return switch (substationLayout) {
            case DiagramConstants.SUBSTATION_LAYOUT_HORIZONTAL -> new HorizontalSubstationLayoutFactory();
            case DiagramConstants.SUBSTATION_LAYOUT_VERTICAL -> new VerticalSubstationLayoutFactory();
            default -> throw new SingleLineDiagramBusinessException(INVALID_SUBSTATION_LAYOUT, "Substation layout " + substationLayout + " incorrect");
        };
    }

    SvgAndMetadata generateSvgAndMetadata(UUID networkUuid, String variantId, String id, SldRequestInfos sldRequestInfos) {
        Network network = getNetwork(networkUuid, variantId, networkStoreService);
        if (network.getVoltageLevel(id) == null && network.getSubstation(id) == null) {
            throw new SingleLineDiagramBusinessException(EQUIPMENT_NOT_FOUND, "Voltage level or substation " + id + " not found");
        }

        try (var svgWriter = new StringWriter();
             var metadataWriter = new StringWriter()) {

            SldComponentLibrary compLibrary = SldComponentLibrary.find(sldRequestInfos.getComponentLibrary())
                    .orElseThrow(() -> new SingleLineDiagramRuntimeException("Component library '" + sldRequestInfos.getComponentLibrary() + "' not found"));

            SvgParameters svgParameters = new SvgParameters(SVG_PARAMETERS);
            svgParameters.setLabelCentered(sldRequestInfos.isCenterLabel());
            svgParameters.setLabelDiagonal(sldRequestInfos.isDiagonalLabel());
            svgParameters.setUseName(sldRequestInfos.isUseName());
            svgParameters.setUndefinedValueSymbol("\u2014");
            svgParameters.setLanguageTag(sldRequestInfos.getLanguage());
            svgParameters.setUnifyVoltageLevelColors(true);
            LayoutParameters layoutParameters = new LayoutParameters(LAYOUT_PARAMETERS);
            layoutParameters.setSpaceForFeederInfos(80);

            SldParameters sldParameters = new SldParameters();

            switch (sldRequestInfos.getSldDisplayMode()) {
                case SldDisplayMode.FEEDER_POSITION:
                    svgParameters.setBusesLegendAdded(false);
                    svgParameters.setLabelDiagonal(true);
                    sldParameters.setLabelProviderFactory(PositionDiagramLabelProvider.newLabelProviderFactory(id));
                    break;
                case SldDisplayMode.STATE_VARIABLE:
                    svgParameters.setBusesLegendAdded(true);
                    sldParameters.setLabelProviderFactory(CommonLabelProvider::new);
                    sldParameters.setLegendWriterFactory(CommonLegendWriter.createFactory(sldRequestInfos.getBusIdToIccValues()));
                    break;
                default:
                    throw new SingleLineDiagramBusinessException(INVALID_CONFIG_REQUEST, String.format("Given sld display mode %s doesn't exist", sldRequestInfos.getSldDisplayMode()));
            }

            var voltageLevelLayoutFactory = CustomVoltageLevelLayoutFactoryCreator.newCustomVoltageLevelLayoutFactoryCreator();
            var substationLayoutFactory = getSubstationLayoutFactory(sldRequestInfos.getSubstationLayout());

            sldParameters.setSvgParameters(svgParameters);
            sldParameters.setSubstationLayoutFactory(substationLayoutFactory);
            sldParameters.setVoltageLevelLayoutFactoryCreator(voltageLevelLayoutFactory);
            sldParameters.setLayoutParameters(layoutParameters);

            Map<String, String> limitViolationStyles = DiagramUtils.createLimitViolationStyles(sldRequestInfos.getCurrentLimitViolationsInfos(), OVERLOAD_STYLE_CLASS);

            sldParameters.setStyleProviderFactory((net, parameters) -> {
                sldRequestInfos.getBaseVoltagesConfigInfos().forEach(vl -> vl.setProfile(DiagramConstants.BASE_VOLTAGES_DEFAULT_PROFILE));
                BaseVoltagesConfig baseVoltagesConfig = new BaseVoltagesConfig();
                baseVoltagesConfig.setBaseVoltages(sldRequestInfos.getBaseVoltagesConfigInfos());
                baseVoltagesConfig.setDefaultProfile(DiagramConstants.BASE_VOLTAGES_DEFAULT_PROFILE);

                return new StyleProvidersList(
                    sldRequestInfos.isTopologicalColoring()
                        ? new TopologicalStyleProvider(baseVoltagesConfig, network, parameters)
                        : new NominalVoltageStyleProvider(baseVoltagesConfig),
                    new HighlightLineStateStyleProvider(network),
                    new LimitHighlightStyleProvider(network, limitViolationStyles),
                    new BusLegendStyleProvider()
                );
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
            throw new SingleLineDiagramBusinessException(INVALID_EQUIPMENT, "Given id '" + id + "' is not a substation or voltage level id in given network '" + network.getId() + "'");
        }
    }

    Collection<String> getAvailableSvgComponentLibraries() {
        return SldComponentLibrary.findAll().stream().map(SldComponentLibrary::getName).collect(Collectors.toList());
    }
}
