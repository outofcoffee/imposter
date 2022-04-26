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
package io.gatehill.imposter.store.service

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.lifecycle.ScriptLifecycleHooks
import io.gatehill.imposter.lifecycle.ScriptLifecycleListener
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.plugin.config.system.StoreConfig
import io.gatehill.imposter.plugin.config.system.SystemConfigHolder
import io.gatehill.imposter.script.ExecutionContext
import io.gatehill.imposter.store.factory.StoreFactory
import io.gatehill.imposter.store.model.StoreHolder
import io.gatehill.imposter.store.util.StoreUtil
import io.gatehill.imposter.util.MapUtil
import io.gatehill.imposter.util.ResourceUtil
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.Paths
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class StoreServiceImpl @Inject constructor(
    private val storeFactory: StoreFactory,
    engineLifecycle: EngineLifecycleHooks,
    scriptLifecycle: ScriptLifecycleHooks,
) : StoreService, EngineLifecycleListener, ScriptLifecycleListener {

    init {
        LOGGER.trace("Stores enabled")
        engineLifecycle.registerListener(this)
        scriptLifecycle.registerListener(this)
    }

    override fun afterRoutesConfigured(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        router: HttpRouter
    ) {
        preloadStores(allPluginConfigs)
    }

    private fun preloadStores(allPluginConfigs: List<PluginConfig>) {
        allPluginConfigs.filter { it is SystemConfigHolder }.forEach { pluginConfig: PluginConfig ->
            val storeConfigs = (pluginConfig as SystemConfigHolder).systemConfig?.storeConfigs
            storeConfigs?.forEach { (storeName: String, storeConfig: StoreConfig) ->
                preload(storeName, pluginConfig, storeConfig)
            }
        }
    }

    private fun preload(storeName: String, pluginConfig: PluginConfig, storeConfig: StoreConfig) {
        // validate config
        check(!StoreUtil.isRequestScopedStore(storeName)) { "Cannot preload request scoped store: $storeName" }

        val store = storeFactory.getStoreByName(storeName, false)

        storeConfig.preloadData?.let { preloadData ->
            LOGGER.trace("Preloading inline data into store: {}", storeName)
            preloadData.forEach { (key: String, value: Any?) -> store.save(key, value) }
            LOGGER.debug("Preloaded {} items from inline data into store: {}", preloadData.size, storeName)

        } ?: storeConfig.preloadFile?.let { preloadFile ->
            check(preloadFile.endsWith(".json")) { "Only JSON (.json) files containing a top-level object are supported for preloading" }
            val preloadPath = Paths.get(pluginConfig.parentDir.path, preloadFile).toAbsolutePath()
            LOGGER.trace("Preloading file {} into store: {}", preloadPath, storeName)

            try {
                @Suppress("UNCHECKED_CAST")
                val fileContents: Map<String, *> =
                    MapUtil.JSON_MAPPER.readValue(preloadPath.toFile(), HashMap::class.java) as Map<String, *>

                fileContents.forEach { (key, value) -> store.save(key, value) }
                LOGGER.debug(
                    "Preloaded {} items from file {} into store: {}",
                    fileContents.size,
                    preloadPath,
                    storeName
                )
            } catch (e: IOException) {
                throw RuntimeException("Error preloading file $preloadPath into store: $storeName", e)
            }
        }
    }

    override fun beforeBuildingRuntimeContext(
        httpExchange: HttpExchange,
        additionalBindings: MutableMap<String, Any>,
        executionContext: ExecutionContext
    ) {
        // inject store object into script engine
        val requestId = httpExchange.get<String>(ResourceUtil.RC_REQUEST_ID_KEY)!!
        additionalBindings["stores"] = StoreHolder(storeFactory, requestId)
    }

    override fun afterResponseSent(httpExchange: HttpExchange, resourceConfig: ResourceConfig?) {
        // clean up request store if one exists
        httpExchange.get<String?>(ResourceUtil.RC_REQUEST_ID_KEY)?.let { uniqueRequestId: String ->
            storeFactory.clearStore(
                StoreUtil.buildRequestStoreName(uniqueRequestId), ephemeral = true
            )
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(StoreServiceImpl::class.java)
    }
}
