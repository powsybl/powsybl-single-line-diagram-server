/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server;

import io.micrometer.context.ContextExecutorService;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed Benrejeb <mohamed.ben-rejeb at rte-france.com>
 */
class NetworkAreaExecutionServiceTest {

    private static final String THREAD_LOCAL_KEY = "network-area-thread-local";
    private final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    @AfterEach
    void tearDown() {
        ContextRegistry.getInstance().removeThreadLocalAccessor(THREAD_LOCAL_KEY);
        threadLocal.remove();
    }

    @Test
    void supplyAsyncPropagatesContext() throws Exception {
        ContextRegistry.getInstance().registerThreadLocalAccessor(new ThreadLocalAccessor<String>() {
            @Override
            public String key() {
                return THREAD_LOCAL_KEY;
            }

            @Override
            public String getValue() {
                return threadLocal.get();
            }

            @Override
            public void setValue(String value) {
                threadLocal.set(value);
            }

            @Override
            public void setValue() {
                threadLocal.remove();
            }
        });

        DiagramGenerationObserver observer = new DiagramGenerationObserver(new SimpleMeterRegistry());
        NetworkAreaExecutionService service = new NetworkAreaExecutionService(1, observer);

        threadLocal.set("expected-context");

        Field executorField = NetworkAreaExecutionService.class.getDeclaredField("executorService");
        executorField.setAccessible(true);
        ExecutorService executorService = (ExecutorService) executorField.get(service);

        assertInstanceOf(ContextExecutorService.class, executorService, "executor should be wrapped in ContextExecutorService");
        assertEquals("expected-context", service.supplyAsync(threadLocal::get).get());
    }
}
