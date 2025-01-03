/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Slimane Amar <slimane.amar at rte-france.com>
 */
@Service
public class DiagramGenerationObserver {
    private static final String OBSERVATION_PREFIX = "app.diagram.";
    private static final String TASK_TYPE_TAG_NAME = "type";
    private static final String TASK_TYPE_TAG_VALUE_CURRENT = "current";
    private static final String TASK_TYPE_TAG_VALUE_PENDING = "pending";
    private static final String TASK_POOL_METER_NAME_PREFIX = OBSERVATION_PREFIX + "tasks.pool.";

    private final MeterRegistry meterRegistry;

    public DiagramGenerationObserver(@NonNull MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void createThreadPoolMetric(ThreadPoolExecutor threadPoolExecutor) {
        Gauge.builder(TASK_POOL_METER_NAME_PREFIX + TASK_TYPE_TAG_VALUE_CURRENT, threadPoolExecutor, ThreadPoolExecutor::getActiveCount)
            .description("The number of active diagram generation tasks in the thread pool")
            .tag(TASK_TYPE_TAG_NAME, TASK_TYPE_TAG_VALUE_CURRENT)
            .register(meterRegistry);
        Gauge.builder(TASK_POOL_METER_NAME_PREFIX + TASK_TYPE_TAG_VALUE_PENDING, threadPoolExecutor, executor -> executor.getQueue().size())
            .description("The number of pending diagram generation tasks in the thread pool")
            .tag(TASK_TYPE_TAG_NAME, TASK_TYPE_TAG_VALUE_PENDING)
            .register(meterRegistry);
    }
}
