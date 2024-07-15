/*
 * Copyright (c) 2024.
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

package io.gatehill.imposter.config.loader.kryo

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.util.ConfigUtil
import io.gatehill.imposter.plugin.DynamicPluginDiscoveryStrategyImpl
import io.gatehill.imposter.plugin.PluginManagerImpl
import io.gatehill.imposter.plugin.config.BasicPluginConfig
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig
import io.gatehill.imposter.plugin.rest.config.RestPluginConfig
import io.gatehill.imposter.plugin.soap.config.SoapPluginConfig
import org.apache.logging.log4j.LogManager

/**
 * Loads YAML/JSON config and snapshots as binary.
 *
 * @author pete
 */
object Snapshotter {
    private val logger = LogManager.getLogger()

    @JvmStatic
    fun main(args: Array<String>) {
        val configDir = arrayOf(args[0])
        logger.info("Config dir: {}", configDir)

        val configFiles = ConfigUtil.discoverConfigFiles(configDir, ConfigUtil.scanRecursiveConfig)
        logger.info("Config files: {}", configFiles)

        val discoveryStrategy = DynamicPluginDiscoveryStrategyImpl()
        val pluginManager = PluginManagerImpl(discoveryStrategy)
        val pluginConfigs = ConfigUtil.readPluginConfigs(pluginManager, configFiles)
        logger.info("Read {} configs", pluginConfigs.size)

        val imposterConfig = ImposterConfig()
        for (pluginConfig in pluginConfigs.values.flatten()) {
            val configClass = determineConfigClass(pluginConfig.plugin)
            logger.info("Parsing {}", configClass.canonicalName)
            val config = ConfigUtil.loadPluginConfig(imposterConfig, pluginConfig, configClass)

            val kryoFile = BinarySerialiser.buildKryoPath(pluginConfig.ref.file)
            BinarySerialiser.serialise(config, kryoFile)
        }
    }

    private fun determineConfigClass(plugin: String): Class<out BasicPluginConfig> {
        return when (plugin) {
            "openapi" -> OpenApiPluginConfig::class.java
            "rest" -> RestPluginConfig::class.java
            "soap" -> SoapPluginConfig::class.java
            else -> throw IllegalStateException("Unsupported plugin: $plugin")
        }
    }
}
