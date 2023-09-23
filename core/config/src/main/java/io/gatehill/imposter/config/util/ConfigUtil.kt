/*
 * Copyright (c) 2016-2023.
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
package io.gatehill.imposter.config.util

import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.ConfigReference
import io.gatehill.imposter.config.LoadedConfig
import io.gatehill.imposter.config.expression.EnvEvaluator
import io.gatehill.imposter.config.model.LightweightConfig
import io.gatehill.imposter.config.resolver.ConfigResolver
import io.gatehill.imposter.expression.eval.ExpressionEvaluator
import io.gatehill.imposter.expression.util.ExpressionUtil
import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.plugin.config.resource.BasePathHolder
import io.gatehill.imposter.util.MapUtil
import io.gatehill.imposter.util.ResourceUtil
import io.gatehill.imposter.util.splitOnCommaAndTrim
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.IOException

/**
 * Utility methods for reading configuration.
 *
 * @author Pete Cornish
 */
object ConfigUtil {
    private val LOGGER = LogManager.getLogger(ConfigUtil::class.java)
    const val CURRENT_PACKAGE = "io.gatehill.imposter"
    private const val CONFIG_FILE_SUFFIX = "-config"
    private const val IGNORE_FILE_NAME = ".imposterignore"

    private val CONFIG_FILE_MAPPERS: Map<String, ObjectMapper> = mapOf(
        "json" to MapUtil.JSON_MAPPER,
        "yaml" to MapUtil.YAML_MAPPER,
        "yml" to MapUtil.YAML_MAPPER
    )

    private val scanRecursiveConfig
        get() = EnvVars.getEnv("IMPOSTER_CONFIG_SCAN_RECURSIVE")?.toBoolean() == true

    private val autoBasePath
        get() = EnvVars.getEnv("IMPOSTER_AUTO_BASE_PATH")?.toBoolean() == true

    private var expressionEvaluators: Map<String, ExpressionEvaluator<*>> = emptyMap()

    private val defaultIgnoreList: List<String> by lazy {
        ConfigUtil::class.java.getResourceAsStream("/$IGNORE_FILE_NAME")?.use {
            parseExclusionsFile(it.reader().readLines())
        } ?: run {
            LOGGER.warn("Failed to find default Imposter ignore file '$IGNORE_FILE_NAME' on classpath")
            emptyList()
        }
    }

    val configResolvers: Set<ConfigResolver>

    /**
     * Controls whether errors encountered during configuration parsing
     * or plugin configuration should be logged instead of thrown.
     * If set to `false` then configuration errors will be thrown instead.
     *
     * Defaults to `false`, as skipping a configuration might also skip
     * the security conditions it contains.
     */
    val ignoreConfigErrors: Boolean
        get() = EnvVars.getEnv("IMPOSTER_IGNORE_CONFIG_ERRORS").toBoolean()

    init {
        configResolvers = registerResolvers()
        initInterpolators(EnvVars.getEnv())
    }

    private fun registerResolvers(): Set<ConfigResolver> {
        val resolvers = MetaUtil.readConfigResolverMetaFiles()
        LOGGER.trace("Configuration resolvers: {}", resolvers)

        return resolvers.map { resolver ->
            try {
                resolver.getDeclaredConstructor().newInstance()
            } catch (e: Exception) {
                throw RuntimeException("Error instantiating configuration resolver: $resolver", e)
            }
        }.toSet()
    }

    /**
     * Parses the `IMPOSTER_CONFIG_DIR` environment variable for a list of configuration directories.
     */
    fun parseConfigDirEnvVar(): Array<String> =
        EnvVars.getEnv("IMPOSTER_CONFIG_DIR")?.splitOnCommaAndTrim()?.toTypedArray() ?: emptyArray()

    fun initInterpolators(environment: Map<String, String>) {
        // reset the environment used by the expression evaluator
        expressionEvaluators = mapOf("env" to EnvEvaluator(environment))
    }

    /**
     * Resolves configuration directories to local paths, then
     * discovers configuration files. File discovery is optionally
     * recursive, defaulting to the value of [scanRecursiveConfig].
     */
    fun discoverConfigFiles(rawConfigDirs: Array<String>, scanRecursive: Boolean = scanRecursiveConfig): List<ConfigReference> {
        val configDirs = resolveToLocal(rawConfigDirs)
        val exclusions = buildExclusions(configDirs)

        val configFiles = mutableListOf<ConfigReference>()
        for (configDir in configDirs) {
            try {
                configFiles += listConfigFiles(configDir, scanRecursive, exclusions)
            } catch (e: Exception) {
                throw RuntimeException("Failed to list config files in: ${configDir}", e)
            }
        }
        return configFiles
    }

    private fun buildExclusions(configDirs: List<File>): List<String> {
        val exclusions = configDirs.flatMap { configDir ->
            val ignoreFile = File(configDir, IGNORE_FILE_NAME)
            if (ignoreFile.exists()) {
                parseExclusionsFile(ignoreFile.readLines())
            } else {
                emptyList()
            }
        }.takeUnless { it.isEmpty() } ?: defaultIgnoreList

        LOGGER.trace("Excluded from config file search: {}", exclusions)
        return exclusions
    }

    private fun parseExclusionsFile(rawEntries: List<String>) =
        rawEntries.filter { it.isNotBlank() && !it.startsWith("#") }

    fun readPluginConfigs(pluginManager: PluginManager, configFiles: List<ConfigReference>): Map<String, List<LoadedConfig>> {
        var configCount = 0

        // read all config files
        val allPluginConfigs = mutableMapOf<String, MutableList<LoadedConfig>>()
        var errorCount = 0

        for (configFile in configFiles) {
            try {
                LOGGER.debug("Loading configuration file: {}", configFile)
                configCount++

                // load to determine plugin
                val config = readPluginConfig(configFile)

                val pluginClass = pluginManager.determinePluginClass(config.plugin)
                val pluginConfigs = allPluginConfigs.getOrPut(pluginClass) { mutableListOf() }
                pluginConfigs.add(config)

            } catch (e: Exception) {
                errorCount++
                val configEx = RuntimeException("Failed to read plugin config: $configFile", e)
                if (ignoreConfigErrors) {
                    LOGGER.warn("Skipping configuration with error", e)
                } else {
                    throw configEx
                }
            }
        }
        LOGGER.trace("Loaded $configCount plugin configuration file(s) with $errorCount error(s): $configFiles")
        return allPluginConfigs
    }

    fun listConfigFiles(configRoot: File, scanRecursive: Boolean, exclusions: List<String>): List<ConfigReference> {
        return listConfigFiles(configRoot, configRoot, scanRecursive, exclusions)
    }

    private fun listConfigFiles(configRoot: File, configDir: File, scanRecursive: Boolean, exclusions: List<String>): List<ConfigReference> {
        val configFiles = mutableListOf<ConfigReference>()

        configDir.listFiles { _, filename -> !exclusions.contains(filename) }?.forEach { file ->
            if (isConfigFile(file.name)) {
                configFiles += ConfigReference(file, configRoot)
            } else {
                if (scanRecursive && file.isDirectory) {
                    configFiles += listConfigFiles(configRoot, file, scanRecursive, exclusions)
                }
            }
        }

        LOGGER.trace("Configuration files discovered in {}: {}", configDir, configFiles)
        return configFiles
    }

    private fun resolveToLocal(rawConfigDirs: Array<String>): List<File> {
        return rawConfigDirs.flatMap { rawDir ->
            val resolvers = configResolvers.filter { it.handles(rawDir) }
            if (resolvers.isEmpty()) throw IllegalStateException("No config resolver can handle path: $rawDir")
            return@flatMap resolvers.map { it.resolve(rawDir) }
        }
    }

    private fun isConfigFile(name: String): Boolean {
        return CONFIG_FILE_MAPPERS.keys.any { extension -> name.endsWith("$CONFIG_FILE_SUFFIX.$extension") }
    }

    /**
     * Reads the contents of the configuration file, performing necessary string substitutions.
     *
     * @param configRef             the configuration reference
     * @return the loaded configuration
     */
    internal fun readPluginConfig(configRef: ConfigReference): LoadedConfig {
        val configFile = configRef.file
        try {
            val rawContents = configFile.readText()
            val parsedContents = ExpressionUtil.eval(rawContents, expressionEvaluators, nullifyUnsupported = false)

            val config = lookupMapper(configFile).readValue(parsedContents, LightweightConfig::class.java)
            config.plugin
                ?: throw IllegalStateException("No plugin specified in configuration file: $configFile")

            return LoadedConfig(configRef, parsedContents, config.plugin)

        } catch (e: JsonMappingException) {
            throw RuntimeException("Error reading configuration file: " + configFile.absolutePath + ", reason: ${e.message}")
        } catch (e: IOException) {
            throw RuntimeException("Error reading configuration file: " + configFile.absolutePath, e)
        }
    }

    /**
     * Converts the raw configuration into a typed configuration object.
     *
     * @param imposterConfig             the imposter configuration
     * @param loadedConfig             the loaded configuration
     * @param configClass            the configuration class
     * @return the configuration
     */
    fun <T : PluginConfigImpl> loadPluginConfig(
        imposterConfig: ImposterConfig,
        loadedConfig: LoadedConfig,
        configClass: Class<T>,
    ): T {
        val configFile = loadedConfig.ref.file
        try {
            val config = lookupMapper(configFile).readValue(loadedConfig.serialised, configClass)!!
            check(config.plugin != null) { "No plugin specified in configuration file: $configFile" }
            config.dir = configFile.parentFile

            // convert any OpenAPI format path parameters
            if (config is ResourcesHolder<*>) {
                (config as ResourcesHolder<*>).resources?.forEach { resource ->
                    resource.path = ResourceUtil.convertPathFromOpenApi(resource.path)
                }
            }

            if (config is BasePathHolder) {
                val basePath = if (autoBasePath) {
                    // Use the relative path from the config root to the config file's directory.
                    // Normalise the path separators to forward slashes.
                    configFile.canonicalPath.substring(loadedConfig.ref.configRoot.canonicalPath.length)
                        .substringBeforeLast(File.separator)
                        .replace('\\', '/')
                } else {
                    config.basePath
                }
                basePath?.let { applyBasePath<T>(basePath, configFile, config) }
            }

            if (imposterConfig.useEmbeddedScriptEngine) {
                config.responseConfig.scriptFile = "embedded"
            }

            return config

        } catch (e: JsonMappingException) {
            throw RuntimeException("Error loading configuration file: " + configFile.absolutePath + ", reason: ${e.message}")
        } catch (e: IOException) {
            throw RuntimeException("Error loading configuration file: " + configFile.absolutePath, e)
        }
    }

    /**
     * Determine the mapper to use based on the filename.
     *
     * @param configFile the configuration file
     * @return the mapper
     */
    private fun lookupMapper(configFile: File): ObjectMapper {
        val extension = configFile.name.substringAfterLast(".")
        return CONFIG_FILE_MAPPERS[extension]
            ?: throw IllegalStateException("Unable to locate mapper for config file: $configFile")
    }

    private fun <T : PluginConfigImpl> applyBasePath(basePath: String, configFile: File, config: T) {
        LOGGER.trace("Using base path '{}' for config file {}", basePath, configFile)
        if (!config.path.isNullOrEmpty() || config.responseConfig.hasConfiguration()) {
            config.path = basePath + (config.path ?: "")
        }
        if (config is ResourcesHolder<*>) {
            config.resources?.forEach { resource ->
                resource.path = basePath + (resource.path ?: "")
            }
        }
    }
}
