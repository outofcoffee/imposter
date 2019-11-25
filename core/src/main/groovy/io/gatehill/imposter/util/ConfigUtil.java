package io.gatehill.imposter.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.plugin.config.PluginConfigImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static io.gatehill.imposter.util.MapUtil.JSON_MAPPER;
import static io.gatehill.imposter.util.MapUtil.YAML_MAPPER;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

/**
 * Utility methods for reading configuration.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class ConfigUtil {
    private static final Logger LOGGER = LogManager.getLogger(ConfigUtil.class);

    public static final String CURRENT_PACKAGE = "io.gatehill.imposter";

    private static final String CONFIG_FILE_SUFFIX = "-config";

    private static final Map<String, ObjectMapper> CONFIG_FILE_MAPPERS = new HashMap<String, ObjectMapper>() {{
        put(".json", JSON_MAPPER);
        put(".yaml", YAML_MAPPER);
        put(".yml", YAML_MAPPER);
    }};

    private ConfigUtil() {
    }

    public static Map<String, List<File>> loadPluginConfigs(PluginManager pluginManager, String[] configDirs) {
        int configCount = 0;

        // read all config files
        final Map<String, List<File>> allPluginConfigs = Maps.newHashMap();
        for (String configDir : configDirs) {
            try {
                final File[] configFiles = ofNullable(new File(configDir).listFiles(ConfigUtil::isConfigFile)).orElse(new File[0]);

                for (File configFile : configFiles) {
                    LOGGER.debug("Loading configuration file: {}", configFile);
                    configCount++;

                    final PluginConfigImpl config = lookupMapper(configFile).readValue(configFile, PluginConfigImpl.class);
                    config.setParentDir(configFile.getParentFile());

                    final String pluginClass = pluginManager.determinePluginClass(config.getPlugin());
                    List<File> pluginConfigs = allPluginConfigs.get(pluginClass);
                    if (isNull(pluginConfigs)) {
                        pluginConfigs = newArrayList();
                        allPluginConfigs.put(pluginClass, pluginConfigs);
                    }

                    pluginConfigs.add(configFile);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        LOGGER.info("Loaded {} plugin configuration file(s) from: {}",
                configCount, Arrays.toString(configDirs));

        return allPluginConfigs;
    }

    private static boolean isConfigFile(File dir, String name) {
        return CONFIG_FILE_MAPPERS.keySet().stream()
                .anyMatch(extension -> name.endsWith(CONFIG_FILE_SUFFIX + extension));
    }

    /**
     * Determine the mapper to use based on the filename.
     *
     * @param configFile the configuration file
     * @return the mapper
     */
    public static ObjectMapper lookupMapper(File configFile) {
        final String extension = configFile.getName().substring(configFile.getName().lastIndexOf("."));
        return ofNullable(CONFIG_FILE_MAPPERS.get(extension))
                .orElseThrow(() -> new IllegalStateException("Unable to locate mapper for config file: " + configFile));
    }
}
