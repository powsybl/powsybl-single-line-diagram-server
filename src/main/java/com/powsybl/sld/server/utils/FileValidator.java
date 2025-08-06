/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.sld.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;
import org.supercsv.io.CsvMapReader;
import org.supercsv.prefs.CsvPreference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

/**
 * @author AJELLAL Ali <ali.ajellal@rte-france.com>
 */
public final class FileValidator {

    private FileValidator() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FileValidator.class);
    public static final CsvPreference CSV_PREFERENCE = new CsvPreference.Builder('"', ';', System.lineSeparator()).build();
    static final String TYPE = "text/csv";

    public static final String VOLTAGE_LEVEL_ID = "voltageLevelId";
    public static final String EQUIPMENT_TYPE = "equipmentType";
    public static final String X_POSITION = "xPosition";
    public static final String Y_POSITION = "yPosition";
    public static final String X_LABEL_POSITION = "xLabelPosition";
    public static final String Y_LABEL_POSITION = "yLabelPosition";
    private static final List<String> POSITIONS_EXPECTED_HEADERS = List.of(VOLTAGE_LEVEL_ID, EQUIPMENT_TYPE, X_POSITION, Y_POSITION, X_LABEL_POSITION, Y_LABEL_POSITION);

    public static boolean validateHeaders(MultipartFile file) {
        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(InputUtils.toBomInputStream(file.getInputStream()), StandardCharsets.UTF_8));
             CsvMapReader mapReader = new CsvMapReader(fileReader, CSV_PREFERENCE)) {
            final List<String> headers = List.of(mapReader.getHeader(true));
            return new HashSet<>(headers).containsAll(POSITIONS_EXPECTED_HEADERS);
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            return false;
        }
    }

    public static boolean hasCSVFormat(MultipartFile file) {
        return FileValidator.TYPE.equals(file.getContentType());
    }
}
