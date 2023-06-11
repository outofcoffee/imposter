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
package io.gatehill.imposter.plugin

import com.google.inject.Injector
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.ConfigReference
import io.gatehill.imposter.plugin.config.ConfigurablePlugin
import org.apache.logging.log4j.LogManager
import java.util.Collections

/**
 * Registers plugins with the injector and triggers configuration.
 *
 * @author Pete Cornish
 */
class PluginManagerImpl(
    private val discoveryStrategy: PluginDiscoveryStrategy,
) : PluginManager {
    private val plugins = mutableMapOf<String, Plugin>()

    override fun preparePluginsFromConfig(
        imposterConfig: ImposterConfig,
        plugins: List<String>,
        pluginConfigs: Map<String, List<ConfigReference>>
    ): List<PluginDependencies> {
        return discoveryStrategy.preparePluginsFromConfig(imposterConfig, plugins, pluginConfigs)
    }

    /**
     * Determines the plugin class if it matches its short name, otherwise assumes
     * the plugin is a fully qualified class name.
     *
     * @param plugin the plugin short name or fully qualified class name
     * @return the fully qualified plugin class name
     */
    override fun determinePluginClass(plugin: String): String {
        return discoveryStrategy.determinePluginClass(plugin)
    }

    /**
     * Instantiate all plugins and register them with the plugin manager, then
     * send config to plugins.
     *
     * @param injector the injector from which the plugins can be instantiated
     * @param pluginConfigs configurations keyed by plugin
     */
    override fun startPlugins(injector: Injector, pluginConfigs: Map<String, List<ConfigReference>>) {
        LOGGER.trace("Starting plugins with {} configs", pluginConfigs.size)
        createPlugins(injector)
        configurePlugins(pluginConfigs)
    }

    /**
     * Instantiate all plugins and register them with the plugin manager.
     *
     * @param injector the injector from which the plugins can be instantiated
     */
    private fun createPlugins(injector: Injector) {
        discoveryStrategy.getPluginClasses().forEach { pluginClass: Class<out Plugin> ->
            try {
                val instance = injector.getInstance(pluginClass)
                plugins[pluginClass.canonicalName] = instance
            } catch (e: Exception) {
                throw RuntimeException("Error registering plugin: $pluginClass", e)
            }
        }

        val allPlugins = getPlugins()
        when (val pluginCount = allPlugins.size) {
            0 -> throw IllegalStateException("No plugins were loaded")
            else -> if (LOGGER.isDebugEnabled) {
                val pluginNames = allPlugins.joinToString(", ", "[", "]") { p: Plugin ->
                    discoveryStrategy.getPluginName(p.javaClass)
                }
                LOGGER.debug("Loaded $pluginCount plugin(s): $pluginNames")
            }
        }
    }

    /**
     * Send config to plugins.
     *
     * @param pluginConfigs configurations keyed by plugin
     */
    private fun configurePlugins(pluginConfigs: Map<String, List<ConfigReference>>) {
        getPlugins().filterIsInstance<ConfigurablePlugin<*>>().forEach { plugin ->
                try {
                    val configFiles = pluginConfigs[plugin.javaClass.canonicalName] ?: emptyList()
                    plugin.loadConfiguration(configFiles)
                } catch (e: Exception) {
                    val pluginName = discoveryStrategy.getPluginName(plugin.javaClass)
                    throw RuntimeException("Error configuring plugin: $pluginName", e)
                }
            }
    }

    override fun getPlugins(): Collection<Plugin> {
        return Collections.unmodifiableCollection(plugins.values)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <P : Plugin?> getPlugin(pluginClassName: String): P? {
        return plugins[pluginClassName] as P?
    }

    companion object {
        private val LOGGER = LogManager.getLogger(PluginManager::class.java)
    }
}
