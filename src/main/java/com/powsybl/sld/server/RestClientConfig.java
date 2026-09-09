/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.sld.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * @author Maissa SOUISSI <maissa.souissi at rte-france.com>
 */

@Configuration
public class RestClientConfig {

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public RestClient restClient(RestClient.Builder restClientBuilder) {
        return restClientBuilder.messageConverters(messageConverters -> {
            //find and replace Jackson message converter with our own
            for (int i = 0; i < messageConverters.size(); i++) {
                final HttpMessageConverter<?> httpMessageConverter = messageConverters.get(i);
                if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
                    messageConverters.set(i, mappingJackson2HttpMessageConverter());
                }
            }
        }).build();
    }

    private MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        return converter;
    }

}
