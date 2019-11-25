package io.gatehill.imposter.plugin.openapi.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.converter.SwaggerConverter;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static io.gatehill.imposter.util.MapUtil.JSON_MAPPER;
import static io.gatehill.imposter.util.MapUtil.YAML_MAPPER;
import static java.util.Optional.ofNullable;
import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

/**
 * @author pete
 */
public final class OpenApiVersionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiVersionUtil.class);

    private OpenApiVersionUtil() {
    }

    private enum SpecVersion {
        V2,
        V3
    }

    public static OpenAPI parseSpecification(OpenApiPluginConfig config) {
        final Path specPath = Paths.get(config.getParentDir().getAbsolutePath(), config.getSpecFile());

        final String specData;
        final Map parsed;
        try {
            specData = FileUtils.readFileToString(specPath.toFile());

            // determine serialisation
            final ObjectMapper mapper = determineMapper(specPath, specData);
            parsed = mapper.readValue(specData, HashMap.class);

        } catch (IOException e) {
            throw new RuntimeException(String.format("Error preparsing specification: %s", specPath), e);
        }

        // determine version
        final SpecVersion specVersion = determineVersion(specPath, parsed);
        LOGGER.debug("Using version: {} parser for: {}", specVersion, specPath);

        // convert or parse directly
        final SwaggerParseResult parseResult;
        switch (specVersion) {
            case V2:
                parseResult = new SwaggerConverter().readContents(
                        specData, Collections.emptyList(), new ParseOptions());
                break;

            case V3:
                parseResult = new OpenAPIV3Parser().readContents(
                        specData, Collections.emptyList(), new ParseOptions());
                break;

            default:
                throw new IllegalStateException(String.format("Unsupported version: %s for: %s",
                        specVersion, specPath));
        }

        if (null == parseResult) {
            throw new IllegalStateException(String.format("Unable to parse specification: %s",
                    specPath));
        }

        if (null != parseResult.getMessages() && !parseResult.getMessages().isEmpty()) {
            LOGGER.info("OpenAPI parser messages for: {}: {}", specPath,
                    parseResult.getMessages().stream().collect(Collectors.joining(LINE_SEPARATOR)));
        }

        if (null == parseResult.getOpenAPI()) {
            throw new IllegalStateException(String.format("Unable to parse specification: %s",
                    specPath));
        }

        return parseResult.getOpenAPI();
    }

    private static ObjectMapper determineMapper(Path specPath, String specData) {
        LOGGER.trace("Determining serialisation for: {}", specPath);

        final ObjectMapper mapper;
        if (specData.trim().startsWith("{")) {
            mapper = JSON_MAPPER;
        } else {
            mapper = YAML_MAPPER;
        }
        return mapper;
    }

    private static SpecVersion determineVersion(Path specPath, Map parsed) {
        LOGGER.trace("Determining version for: {}", specPath);

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
            LOGGER.warn("Could not determine version for: {} - guessing V3", specPath);
            return SpecVersion.V3;
        }
    }
}
