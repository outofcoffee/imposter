/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.gatehill.imposter.plugin.openapi.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.converter.SwaggerConverter;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.swagger.v3.parser.util.RemoteUrl;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static io.gatehill.imposter.util.MapUtil.JSON_MAPPER;
import static io.gatehill.imposter.util.MapUtil.YAML_MAPPER;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

/**
 * Utility functions to load the OpenAPI specification, determining the version and use the appropriate parser.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class SpecificationLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpecificationLoader.class);

    private SpecificationLoader() {
    }

    private enum SpecVersion {
        V2,
        V3
    }

    public static OpenAPI parseSpecification(OpenApiPluginConfig config) {
        final String specData = loadSpecData(config);

        // determine serialisation
        final Map<?, ?> parsed;
        try {
            final ObjectMapper mapper = determineMapper(config.getSpecFile(), specData);
            parsed = mapper.readValue(specData, HashMap.class);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error preparsing specification: %s", config.getSpecFile()), e);
        }

        // determine version
        final SpecVersion specVersion = determineVersion(config.getSpecFile(), parsed);
        LOGGER.trace("Using version: {} parser for: {}", specVersion, config.getSpecFile());

        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolveFully(true);

        // convert or parse directly
        final SwaggerParseResult parseResult;
        switch (specVersion) {
            case V2:
                parseResult = new SwaggerConverter().readContents(specData, emptyList(), parseOptions);
                break;
            case V3:
                parseResult = new OpenAPIV3Parser().readContents(specData, emptyList(), parseOptions);
                break;
            default:
                throw new IllegalStateException(
                        String.format("Unsupported version: %s for: %s", specVersion, config.getSpecFile())
                );
        }

        if (null == parseResult) {
            throw new IllegalStateException(String.format("Unable to parse specification: %s", config.getSpecFile()));
        }

        if (null != parseResult.getMessages() && !parseResult.getMessages().isEmpty()) {
            LOGGER.info(
                    "OpenAPI parser messages for: {}: {}",
                    config.getSpecFile(), String.join(System.lineSeparator(), parseResult.getMessages())
            );
        }

        if (null == parseResult.getOpenAPI()) {
            throw new IllegalStateException(String.format("Unable to parse specification: %s", config.getSpecFile()));
        }

        return parseResult.getOpenAPI();
    }

    private static String loadSpecData(OpenApiPluginConfig config) {
        final String specFile = config.getSpecFile();
        if (specFile.startsWith("http://") || specFile.startsWith("https://")) {
            return readSpecFromUrl(specFile);
        } else if (specFile.startsWith("s3://")) {
            return S3SpecificationLoader.getInstance().readSpecFromS3(specFile);
        } else {
            return readSpecFromFile(config, specFile);
        }
    }

    private static String readSpecFromUrl(String specUrl) {
        try {
            final String specData = RemoteUrl.urlToString(specUrl, emptyList());
            LOGGER.debug("Specification read [{} bytes] from URL: {}", specData.length(), specUrl);
            return specData;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Error fetching remote specification from: %s", specUrl), e);
        }
    }

    private static String readSpecFromFile(OpenApiPluginConfig config, String specFile) {
        final Path specPath = Paths.get(config.getParentDir().getAbsolutePath(), specFile);
        try {
            return FileUtils.readFileToString(specPath.toFile(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error reading specification: %s", specPath), e);
        }
    }

    private static ObjectMapper determineMapper(String specFile, String specData) {
        LOGGER.trace("Determining serialisation for: {}", specFile);
        final ObjectMapper mapper;
        if (specData.trim().startsWith("{")) {
            mapper = JSON_MAPPER;
        } else {
            mapper = YAML_MAPPER;
        }
        return mapper;
    }

    private static SpecVersion determineVersion(String specFile, Map parsed) {
        LOGGER.trace("Determining version for: {}", specFile);

        final String versionString = ofNullable(parsed.get("openapi"))
                .orElse(ofNullable(parsed.get("swagger")).orElse(""))
                .toString();

        if (versionString.equals("3") || versionString.startsWith("3.")) {
            // OpenAPI v3
            return SpecVersion.V3;

        } else if (versionString.equals("2") || versionString.startsWith("2.")) {
            // Swagger/OpenAPI v2
            return SpecVersion.V2;

        } else {
            // default to v3
            LOGGER.warn("Could not determine version for: {} - guessing V3", specFile);
            return SpecVersion.V3;
        }
    }
}
