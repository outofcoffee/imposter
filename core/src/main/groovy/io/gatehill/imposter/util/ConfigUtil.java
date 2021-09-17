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

package io.gatehill.imposter.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.plugin.config.PluginConfigImpl;
import io.gatehill.imposter.plugin.config.ResourcesHolder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private static StringSubstitutor placeholderSubstitutor;

    static {
        initInterpolators(System.getenv());
    }

    private ConfigUtil() {
    }

    public static void initInterpolators(Map<String, String> environment) {
        // prefix all environment vars with 'env.'
        final Map<String, String> environmentVars = environment.entrySet().stream()
                .collect(Collectors.toMap(e -> "env." + e.getKey(), Map.Entry::getValue));

        placeholderSubstitutor = new StringSubstitutor(environmentVars);
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

                    // load to determine plugin
                    final PluginConfigImpl config = loadPluginConfig(configFile, PluginConfigImpl.class, false, false);

                    final String pluginClass = pluginManager.determinePluginClass(config.getPlugin());
                    List<File> pluginConfigs = allPluginConfigs.get(pluginClass);
                    if (isNull(pluginConfigs)) {
                        pluginConfigs = newArrayList();
                        allPluginConfigs.put(pluginClass, pluginConfigs);
                    }

                    pluginConfigs.add(configFile);
                }

            } catch (Exception e) {
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
     * Reads the contents of the configuration file, performing necessary string substitutions.
     *
     * @param configFile             the configuration file
     * @param configClass            the configuration class
     * @param substitutePlaceholders whether to substitute placeholders in the configuration
     * @param convertPathParameters  whether to convert path parameters from OpenAPI format to Vert.x format
     * @return the configuration
     */
    public static <T extends PluginConfigImpl> T loadPluginConfig(
            File configFile,
            Class<T> configClass,
            boolean substitutePlaceholders,
            boolean convertPathParameters
    ) {
        try {
            final String rawContents = FileUtils.readFileToString(configFile, StandardCharsets.UTF_8);
            final String parsedContents = substitutePlaceholders ?
                    placeholderSubstitutor.replace(rawContents) :
                    rawContents;

            final T config = lookupMapper(configFile).readValue(parsedContents, configClass);
            config.setParentDir(configFile.getParentFile());

            // convert OpenAPI format path parameters to Vert.x format
            if (convertPathParameters && config instanceof ResourcesHolder) {
                ofNullable(((ResourcesHolder<?>) config).getResources()).ifPresent(resources ->
                        resources.forEach(resource -> resource.setPath(ResourceUtil.convertPathToVertx(resource.getPath())))
                );
            }

            return config;

        } catch (IOException e) {
            throw new RuntimeException("Error reading configuration file: " + configFile.getAbsolutePath(), e);
        }
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
