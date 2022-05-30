/*
 * Copyright (c) 2022-2022.
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

package io.gatehill.imposter

import com.google.inject.Module
import io.gatehill.imposter.plugin.PluginDiscoveryStrategy
import io.gatehill.imposter.util.ClassLoaderUtil
import io.gatehill.imposter.util.FeatureUtil
import io.vertx.core.Vertx
import org.apache.logging.log4j.LogManager

/**
 * Constructs engine instances.
 *
 * @author Pete Cornish
 */
object EngineBuilder {
    private val LOGGER = LogManager.getLogger(EngineBuilder::class.java)

    /**
     * Construct a new engine instance, ready to start.
     *
     * @param vertx             the Vert.x instance to use
     * @param featureModules    conditionally loaded modules, if the corresponding feature is enabled
     * @param additionalModules any [Module]s to bootstrap the engine
     */
    fun newEngine(
        vertx: Vertx,
        imposterConfig: ImposterConfig,
        featureModules: Map<String, Class<out Module>> = emptyMap(),
        vararg additionalModules: Module
    ): Imposter {
        LOGGER.trace("Initialising mock engine")

        val pluginDiscoveryStrategy = getPluginDiscoveryStrategy(imposterConfig)
        val bootstrapModules = FeatureUtil.getModulesForEnabledFeatures(featureModules) + additionalModules

        return Imposter(vertx, imposterConfig, pluginDiscoveryStrategy, bootstrapModules)
    }

    private fun getPluginDiscoveryStrategy(imposterConfig: ImposterConfig): PluginDiscoveryStrategy {
        return imposterConfig.pluginDiscoveryStrategy ?: run {
            try {
                val pluginDiscoveryStrategyClass =
                    ClassLoaderUtil.loadClass<PluginDiscoveryStrategy>(imposterConfig.pluginDiscoveryStrategyClass!!)

                return pluginDiscoveryStrategyClass.getDeclaredConstructor().newInstance()

            } catch (e: Exception) {
                throw RuntimeException(
                    "Error getting plugin discovery strategy '${imposterConfig.pluginDiscoveryStrategyClass}'", e
                )
            }
        }
    }
}
