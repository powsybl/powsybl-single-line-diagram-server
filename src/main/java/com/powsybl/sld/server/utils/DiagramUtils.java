/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.utils;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.server.dto.LimitViolationInfos;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.Locale;

/**
 * @author Etienne Homer<etienne.homer at rte-france.com>
 */
public final class DiagramUtils {
    private DiagramUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    public static Network getNetwork(UUID networkUuid, String variantId, NetworkStoreService networkStoreService, PreloadingStrategy preloadingStrategy) {
        try {
            Network network = networkStoreService.getNetwork(networkUuid, preloadingStrategy);
            if (variantId != null) {
                network.getVariantManager().setWorkingVariant(variantId);
            }
            return network;
        } catch (PowsyblException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Creates a map of equipment ID to CSS style class for limit violations.
     *
     * @param limitViolationInfos Set of limit violation information
     * @param baseStyleClass Base CSS class for violations (e.g., "sld-overload" or "nad-overloaded")
     * @return Map from equipment ID to CSS style class, or empty map if no violations
     */
    public static Map<String, String> createViolationStylesMap(Set<LimitViolationInfos> limitViolationInfos, String baseStyleClass) {
        if (limitViolationInfos == null || limitViolationInfos.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> violationStyles = new HashMap<>();
        for (LimitViolationInfos li : limitViolationInfos) {
            String limitName = li.getLimitName();
            String styleClass;
            if (limitName == null || limitName.isBlank()) {
                styleClass = baseStyleClass;
            } else {
                // Sanitize limit name to create a safe CSS class
                String safe = limitName.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
                styleClass = baseStyleClass + "-" + safe;
            }
            violationStyles.put(li.getEquipmentId(), styleClass);
        }
        return violationStyles;
    }
}
