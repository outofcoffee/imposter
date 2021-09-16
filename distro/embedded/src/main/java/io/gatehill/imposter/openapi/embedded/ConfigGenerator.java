package io.gatehill.imposter.openapi.embedded;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

/**
 * Generates Imposter configuration for the OpenAPI plugin.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class ConfigGenerator {
    private static final Logger LOGGER = LogManager.getLogger(ConfigGenerator.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static Path writeImposterConfig(List<Path> specificationFiles) throws IOException {
        final Path configDir = Files.createTempDirectory("imposter");
        specificationFiles.forEach(spec -> {
            try {
                // copy spec into place
                Files.copy(spec, configDir.resolve(spec.getFileName()));

                // write config file
                final Path configFile = configDir.resolve(spec.getFileName() + "-config.json");
                try (final FileOutputStream out = new FileOutputStream(configFile.toFile())) {
                    MAPPER.writeValue(out, new HashMap<String, Object>() {{
                        put("plugin", "openapi");
                        put("specFile", spec.getFileName().toString());
                    }});
                }
                LOGGER.debug("Wrote Imposter configuration file: {}", configFile);

            } catch (IOException e) {
                throw new RuntimeException(String.format("Error generating configuration for specification file %s in %s", spec, configDir), e);
            }
        });
        return configDir;
    }
}
