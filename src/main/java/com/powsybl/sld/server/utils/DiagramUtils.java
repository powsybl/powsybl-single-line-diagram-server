/**
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.utils;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.iidm.network.VoltageLevel;
import com.powsybl.iidm.network.extensions.ConnectablePosition;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import com.powsybl.sld.server.SldException;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

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

    public static Integer getInjectionOrder(ConnectablePosition<?> position, VoltageLevel voltageLevel, Injection<?> injection, boolean throwException, Logger logger) {
        Integer singleOrder = position.getFeeder().getOrder().orElse(null);
        checkConnectableInVoltageLevel(singleOrder, voltageLevel, injection, throwException, logger);
        return singleOrder;
    }

    public static Integer getBranchOrders(ConnectablePosition<?> position, VoltageLevel voltageLevel, Branch<?> branch, boolean throwException, Logger logger) {
        Integer order;
        if (branch.getTerminal1().getVoltageLevel() == voltageLevel) {
            order = position.getFeeder1().getOrder().orElse(null);
        } else if (branch.getTerminal2().getVoltageLevel() == voltageLevel) {
            order = position.getFeeder2().getOrder().orElse(null);
        } else {
            throw new SldException(String.format("Given voltageLevel %s not found in terminal 1 or terminal 2 of branch", voltageLevel.getId()));
        }
        checkConnectableInVoltageLevel(order, voltageLevel, branch, throwException, logger);
        return order;
    }

    public static Integer get3wtOrder(ConnectablePosition<?> position, VoltageLevel voltageLevel, ThreeWindingsTransformer twt, boolean throwException, Logger logger) {
        Integer order;
        if (twt.getLeg1().getTerminal().getVoltageLevel() == voltageLevel) {
            order = position.getFeeder1().getOrder().orElse(null);
        } else if (twt.getLeg2().getTerminal().getVoltageLevel() == voltageLevel) {
            order = position.getFeeder2().getOrder().orElse(null);
        } else if (twt.getLeg3().getTerminal().getVoltageLevel() == voltageLevel) {
            order = position.getFeeder3().getOrder().orElse(null);
        } else {
            throw new SldException(String.format("Given voltageLevel %s not found in leg 1, leg 2 or leg 3 of ThreeWindingsTransformer", voltageLevel.getId()));
        }
        checkConnectableInVoltageLevel(order, voltageLevel, twt, throwException, logger);
        return order;
    }

    public static Integer getOrderPositions(ConnectablePosition<?> position, VoltageLevel voltageLevel, Identifiable<?> identifiable, boolean throwException, Logger logger) {
        if (identifiable instanceof Injection) {
            return getInjectionOrder(position, voltageLevel, (Injection<?>) identifiable, throwException, logger);
        } else if (identifiable instanceof Branch) {
            return getBranchOrders(position, voltageLevel, (Branch<?>) identifiable, throwException, logger);
        } else if (identifiable instanceof ThreeWindingsTransformer) {
            return get3wtOrder(position, voltageLevel, (ThreeWindingsTransformer) identifiable, throwException, logger);
        } else {
            logger.error("Given connectable not supported: {}", identifiable.getClass().getName());
            if (throwException) {
                throw new SldException(String.format("Given connectable %s not supported: ", identifiable.getClass().getName()));
            }
        }
        return null;
    }

    public static void checkConnectableInVoltageLevel(Integer order, VoltageLevel voltageLevel, Connectable<?> connectable, boolean throwException, Logger logger) {
        if (order == null) {
            logger.error("Given connectable {} not found in voltageLevel {}", connectable.getId(), voltageLevel.getId());
            if (throwException) {
                throw new SldException(String.format("Given connectable %s not found in voltageLevel %s ", connectable.getId(), voltageLevel.getId()));
            }
        }
    }
}
