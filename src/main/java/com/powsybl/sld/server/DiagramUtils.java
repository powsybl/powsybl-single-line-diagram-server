package com.powsybl.sld.server;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.client.NetworkStoreService;
import com.powsybl.network.store.client.PreloadingStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

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
}
