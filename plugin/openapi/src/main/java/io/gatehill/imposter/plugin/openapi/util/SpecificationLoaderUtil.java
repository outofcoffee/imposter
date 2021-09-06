package io.gatehill.imposter.plugin.openapi.util;

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
 * Utility functions to determine OpenAPI version and use the appropriate parser.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class SpecificationLoaderUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpecificationLoaderUtil.class);

    private SpecificationLoaderUtil() {
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
        LOGGER.debug("Using version: {} parser for: {}", specVersion, config.getSpecFile());

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
        final String specData;
        if (config.getSpecFile().startsWith("http://") || config.getSpecFile().startsWith("https://")) {
            try {
                specData = RemoteUrl.urlToString(config.getSpecFile(), emptyList());
            } catch (Exception e) {
                throw new RuntimeException(String.format("Error fetching remote specification from: %s", config.getSpecFile()), e);
            }
        } else {
            final Path specPath = Paths.get(config.getParentDir().getAbsolutePath(), config.getSpecFile());
            try {
                specData = FileUtils.readFileToString(specPath.toFile(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Error reading specification: %s", specPath), e);
            }
        }
        return specData;
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
