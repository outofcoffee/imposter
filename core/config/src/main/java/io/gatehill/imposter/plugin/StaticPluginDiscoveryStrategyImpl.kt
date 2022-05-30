/*
 * Copyright (c) 2021-2021.
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

import io.gatehill.imposter.ImposterConfig
import org.apache.logging.log4j.LogManager
import java.io.File

/**
 * A static plugin discovery implementation that does not scan the classpath
 * or use `PluginInfo` or `RequireModules` annotations to discover plugin metadata
 * or dependencies.
 *
 * This implementation is much faster than `DynamicPluginDiscoveryStrategyImpl`.
 *
 * @author Pete Cornish
 */
class StaticPluginDiscoveryStrategyImpl(
    private val pluginClasses: Map<String, Class<out Plugin>>,
    private val pluginDependencies: List<com.google.inject.Module>,
) : PluginDiscoveryStrategy {
    private val logger = LogManager.getLogger(StaticPluginDiscoveryStrategyImpl::class.java)

    /**
     * Map of FQCN to plugin name.
     */
    private val pluginNames = pluginClasses.entries.associate { it.value.canonicalName to it.key }

    init {
        if (logger.isTraceEnabled) {
            logger.trace("Using static plugin discovery strategy - plugins: {}", pluginNames)
        } else {
            logger.debug("Using static plugin discovery strategy")
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun preparePluginsFromConfig(
        imposterConfig: ImposterConfig,
        initialPlugins: List<String>,
        pluginConfigs: Map<String, List<File>>
    ): List<PluginDependencies> {
        return listOf(PluginDependencies(pluginDependencies))
    }

    /**
     * {@inheritDoc}
     */
    override fun determinePluginClass(plugin: String): String {
        return pluginClasses[plugin]?.canonicalName ?: plugin
    }

    override fun getPluginClasses(): Collection<Class<out Plugin>> {
        return pluginClasses.values
    }

    override fun getPluginName(clazz: Class<in Plugin>): String {
        return pluginNames[clazz.canonicalName]
            ?: throw IllegalStateException("No plugin name found for class: $clazz")
    }
}
