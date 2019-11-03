package com.gatehill.imposter.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gatehill.imposter.plugin.config.BaseConfig;
import com.google.common.collect.Maps;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.gatehill.imposter.util.FileUtil.CONFIG_FILE_MAPPERS;
import static com.gatehill.imposter.util.FileUtil.CONFIG_FILE_SUFFIX;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;

/**
 * Utility methods for reading configuration.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class ConfigUtil {
    private static final Logger LOGGER = LogManager.getLogger(ConfigUtil.class);

    private ConfigUtil() {
    }

    public static Map<String, List<File>> loadPluginConfigs(String[] configDirs) {
        int configCount = 0;

        // read all config files
        final Map<String, List<File>> allPluginConfigs = Maps.newHashMap();
        for (String configDir : configDirs) {
            try {
                final File[] configFiles = ofNullable(new File(configDir).listFiles(ConfigUtil::isConfigFile)).orElse(new File[0]);

                for (File configFile : configFiles) {
                    LOGGER.debug("Loading configuration file: {}", configFile);
                    configCount++;

                    final BaseConfig config = lookupMapper(configFile).readValue(configFile, BaseConfig.class);
                    config.setParentDir(configFile.getParentFile());

                    List<File> pluginConfigs = allPluginConfigs.get(config.getPluginClass());
                    if (Objects.isNull(pluginConfigs)) {
                        pluginConfigs = newArrayList();
                        allPluginConfigs.put(config.getPluginClass(), pluginConfigs);
                    }

                    pluginConfigs.add(configFile);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        LOGGER.info("Loaded {} plugin configuration files from: {}",
                configCount, Arrays.toString(configDirs));

        return allPluginConfigs;
    }

    private static boolean isConfigFile(File dir, String name) {
        return CONFIG_FILE_MAPPERS.keySet().stream()
                .anyMatch(extension -> name.endsWith(CONFIG_FILE_SUFFIX + extension));
    }

    private static ObjectMapper lookupMapper(File configFile) {
        final String extension = configFile.getName().substring(configFile.getName().lastIndexOf("."));
        return ofNullable(CONFIG_FILE_MAPPERS.get(extension))
                .orElseThrow(() -> new IllegalStateException("Unable to locate mapper for config file: " + configFile));
    }
}
