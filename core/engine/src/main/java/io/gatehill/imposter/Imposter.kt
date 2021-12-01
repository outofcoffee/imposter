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
package io.gatehill.imposter

import com.google.inject.Module
import io.gatehill.imposter.config.util.ConfigUtil
import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.plugin.PluginManagerImpl
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.InjectorUtil
import org.apache.logging.log4j.LogManager
import java.io.File
import java.net.URI
import java.nio.file.Paths

/**
 * @author Pete Cornish
 */
class Imposter constructor(
    private val imposterConfig: ImposterConfig,
    private val bootstrapModules: Array<Module>,
) {
    private val pluginManager: PluginManager = PluginManagerImpl()

    fun start() {
        LOGGER.info("Starting mock engine")

        val pluginConfigs = processConfiguration()
        val plugins = imposterConfig.plugins?.toList() ?: emptyList()
        val dependencies = pluginManager.preparePluginsFromConfig(imposterConfig, plugins, pluginConfigs)

        val allModules = mutableListOf<Module>().apply {
            addAll(bootstrapModules)
            add(ImposterModule(imposterConfig, pluginManager))
            addAll(dependencies.flatMap { it.requiredModules })
        }

        // inject dependencies
        val injector = InjectorUtil.create(*allModules.toTypedArray())
        injector.injectMembers(this)
        pluginManager.registerPlugins(injector)
        pluginManager.configurePlugins(pluginConfigs)
    }

    private fun processConfiguration(): Map<String, MutableList<File>> {
        imposterConfig.serverUrl = buildServerUrl().toString()
        val configDirs = imposterConfig.configDirs

        // resolve relative config paths
        for (i in configDirs.indices) {
            if (configDirs[i].startsWith("./")) {
                configDirs[i] = Paths.get(System.getProperty("user.dir"), configDirs[i].substring(2)).toString()
            }
        }

        return ConfigUtil.loadPluginConfigs(imposterConfig, pluginManager, imposterConfig.configDirs)
    }

    private fun buildServerUrl(): URI {
        // might be set explicitly
        if (imposterConfig.serverUrl != null) {
            return URI.create(imposterConfig.serverUrl!!)
        }

        // build based on configuration
        val scheme = (if (imposterConfig.isTlsEnabled) "https" else "http") + "://"
        val host = if (HttpUtil.BIND_ALL_HOSTS == imposterConfig.host) "localhost" else imposterConfig.host!!
        val port: String = if (shouldHidePort()) "" else ":" + imposterConfig.listenPort
        return URI.create(scheme + host + port)
    }

    private fun shouldHidePort() = (imposterConfig.isTlsEnabled && 443 == imposterConfig.listenPort) ||
            (!imposterConfig.isTlsEnabled && 80 == imposterConfig.listenPort)

    companion object {
        private val LOGGER = LogManager.getLogger(Imposter::class.java)
    }
}