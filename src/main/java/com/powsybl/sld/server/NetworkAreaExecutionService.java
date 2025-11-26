/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.sld.server;

import jakarta.annotation.PreDestroy;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@Service
public class NetworkAreaExecutionService {

    private ThreadPoolExecutor executorService;

    public NetworkAreaExecutionService(@Value("${max-concurrent-nad-generations}") int maxConcurrentNadGenerations,
                                       @NonNull DiagramGenerationObserver diagramGenerationObserver) {
        executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(maxConcurrentNadGenerations);
        diagramGenerationObserver.createThreadPoolMetric(executorService);
    }

    @PreDestroy
    private void preDestroy() {
        executorService.shutdown();
    }

    public <U> CompletableFuture<U> supplyAsync(Supplier<U> supplier) {
        return CompletableFuture.supplyAsync(supplier, executorService);
    }
}
