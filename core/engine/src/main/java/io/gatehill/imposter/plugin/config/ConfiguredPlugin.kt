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
package io.gatehill.imposter.plugin.config

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.util.ConfigUtil
import io.gatehill.imposter.http.UniqueRoute
import io.gatehill.imposter.plugin.RoutablePlugin
import io.vertx.core.Vertx
import java.io.File
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
abstract class ConfiguredPlugin<T : PluginConfigImpl> @Inject constructor(
    protected val vertx: Vertx,
    protected val imposterConfig: ImposterConfig
) : RoutablePlugin, ConfigurablePlugin<T> {

    override var configs: List<T> = emptyList()

    protected abstract val configClass: Class<T>

    override fun loadConfiguration(configFiles: List<File>) {
        configs = configFiles.map { file ->
            ConfigUtil.loadPluginConfig(
                imposterConfig,
                file,
                configClass,
                substitutePlaceholders = true,
                convertPathParameters = true
            )
        }
        configurePlugin(configs)
    }

    /**
     * Strongly typed configuration objects for this plugin.
     *
     * @param configs
     */
    protected open fun configurePlugin(configs: List<T>) {
        /* no op */
    }

    /**
     * Iterates over [configs] to find unique route combinations of path and HTTP method.
     * For each combination found, only the _first_ resource configuration
     * (that is, a plugin configuration or subresource configuration) is returned.
     */
    protected fun findUniqueRoutes(): Map<UniqueRoute, T> {
        val unique = mutableMapOf<UniqueRoute, T>()
        configs.forEach { config ->
            // root resource
            config.path?.let {
                val uniqueRoute = UniqueRoute.fromResourceConfig(config)
                unique[uniqueRoute] = config
            }

            // subresources
            if (config is ResourcesHolder<*>) {
                config.resources?.forEach { resource ->
                    val uniqueRoute = UniqueRoute.fromResourceConfig(resource)
                    if (!unique.containsKey(uniqueRoute)) {
                        unique[uniqueRoute] = config
                    }
                }
            }
        }
        return unique
    }
}
