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

import com.fasterxml.jackson.core.JsonProcessingException
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpExchangeHandler
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.store.model.Store
import io.gatehill.imposter.store.model.StoreFactory
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.MapUtil
import io.vertx.core.Vertx
import org.apache.logging.log4j.LogManager
import java.util.Objects
import javax.inject.Inject

/**
 * Provides CRUD operations on stores and items via a RESTful API.
 *
 * @author Pete Cornish
 */
class StoreRestApiServiceImpl @Inject constructor(
    private val vertx: Vertx,
    private val resourceService: ResourceService,
    private val storeFactory: StoreFactory,
    lifecycleHooks: EngineLifecycleHooks,
) : EngineLifecycleListener {

    init {
        lifecycleHooks.registerListener(this)
    }

    override fun afterRoutesConfigured(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        router: HttpRouter
    ) {
        router.get("/system/store/:storeName").handler(handleLoadAll(imposterConfig, allPluginConfigs))
        router.delete("/system/store/:storeName").handler(handleDeleteStore(imposterConfig, allPluginConfigs))
        router.get("/system/store/:storeName/:key").handler(handleLoadSingle(imposterConfig, allPluginConfigs))
        router.put("/system/store/:storeName/:key").handler(handleSaveSingle(imposterConfig, allPluginConfigs))
        router.post("/system/store/:storeName").handler(handleSaveMultiple(imposterConfig, allPluginConfigs))
        router.delete("/system/store/:storeName/:key").handler(handleDeleteSingle(imposterConfig, allPluginConfigs))
    }

    private fun handleLoadAll(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>
    ): HttpExchangeHandler {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx) { httpExchange: HttpExchange ->
            val storeName = httpExchange.pathParam("storeName")!!
            val store = openStore(httpExchange, storeName)
            if (Objects.isNull(store)) {
                return@handleRoute
            }

            if (httpExchange.isAcceptHeaderEmpty() || httpExchange.acceptsMimeType(HttpUtil.CONTENT_TYPE_JSON)) {
                LOGGER.debug("Listing store: {}", storeName)
                serialiseBodyAsJson(httpExchange, store!!.loadAll())

            } else {
                // client doesn't accept JSON
                LOGGER.warn("Cannot serialise store: {} as client does not accept JSON", storeName)
                httpExchange.response()
                    .setStatusCode(HttpUtil.HTTP_NOT_ACCEPTABLE)
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                    .end("Stores are only available as JSON. Please set an appropriate Accept header.")
            }
        }
    }

    private fun handleDeleteStore(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>
    ): HttpExchangeHandler {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx) { httpExchange: HttpExchange ->
            val storeName = httpExchange.pathParam("storeName")!!
            storeFactory.deleteStoreByName(storeName, isEphemeralStore = false)
            LOGGER.debug("Deleted store: {}", storeName)

            httpExchange.response()
                .setStatusCode(HttpUtil.HTTP_NO_CONTENT)
                .end()
        }
    }

    private fun handleLoadSingle(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>
    ): HttpExchangeHandler {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx) { httpExchange: HttpExchange ->
            val storeName = httpExchange.pathParam("storeName")!!
            val store = openStore(httpExchange, storeName)
            if (Objects.isNull(store)) {
                return@handleRoute
            }

            val key = httpExchange.pathParam("key")!!
            store!!.load<Any>(key)?.let { value ->
                if (value is String) {
                    LOGGER.debug("Returning string item: {} from store: {}", key, storeName)
                    httpExchange.response()
                        .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                        .end(value)
                } else {
                    LOGGER.debug("Returning object item: {} from store: {}", key, storeName)
                    serialiseBodyAsJson(httpExchange, value)
                }

            } ?: run {
                LOGGER.debug("Nonexistent item: {} in store: {}", key, storeName)
                httpExchange.response()
                    .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                    .end()
            }
        }
    }

    private fun handleSaveSingle(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>
    ): HttpExchangeHandler {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx) { httpExchange: HttpExchange ->
            val storeName = httpExchange.pathParam("storeName")!!
            val store = openStore(httpExchange, storeName, true)
            if (Objects.isNull(store)) {
                return@handleRoute
            }
            val key = httpExchange.pathParam("key")!!

            // "If the target resource does not have a current representation and the
            // PUT successfully creates one, then the origin server MUST inform the
            // user agent by sending a 201 (Created) response."
            // See: https://datatracker.ietf.org/doc/html/rfc7231#section-4.3.4
            val statusCode = if (store!!.hasItemWithKey(key)) HttpUtil.HTTP_OK else HttpUtil.HTTP_CREATED

            val value = httpExchange.bodyAsString
            store.save(key, value)
            LOGGER.debug("Saved item: {} to store: {}", key, storeName)

            httpExchange.response()
                .setStatusCode(statusCode)
                .end()
        }
    }

    private fun handleSaveMultiple(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>
    ): HttpExchangeHandler {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx) { httpExchange: HttpExchange ->
            val storeName = httpExchange.pathParam("storeName")!!
            val store = openStore(httpExchange, storeName, true)
            if (Objects.isNull(store)) {
                return@handleRoute
            }

            val items = httpExchange.bodyAsJson
            items?.forEach { (key: String, value: Any?) -> store!!.save(key, value) }
            val itemCount = items?.size() ?: 0
            LOGGER.debug("Saved {} items to store: {}", itemCount, storeName)

            httpExchange.response()
                .setStatusCode(HttpUtil.HTTP_OK)
                .end()
        }
    }

    private fun handleDeleteSingle(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>
    ): HttpExchangeHandler {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx) { httpExchange: HttpExchange ->
            val storeName = httpExchange.pathParam("storeName")!!
            val store = openStore(httpExchange, storeName, true)
            if (Objects.isNull(store)) {
                return@handleRoute
            }

            val key = httpExchange.pathParam("key")!!
            store!!.delete(key)
            LOGGER.debug("Deleted item: {} from store: {}", key, storeName)

            httpExchange.response()
                .setStatusCode(HttpUtil.HTTP_NO_CONTENT)
                .end()
        }
    }

    private fun openStore(
        httpExchange: HttpExchange,
        storeName: String,
        createIfNotExist: Boolean = false
    ): Store? {
        if (!storeFactory.hasStoreWithName(storeName)) {
            LOGGER.debug("No store found named: {}", storeName)
            if (!createIfNotExist) {
                httpExchange.response()
                    .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                    .end("No store named '$storeName'.")
                return null
            }
            // ...otherwise fall through and implicitly create below
        }
        return storeFactory.getStoreByName(storeName, false)
    }

    private fun serialiseBodyAsJson(httpExchange: HttpExchange, body: Any?) {
        try {
            httpExchange.response()
                .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                .end(MapUtil.JSON_MAPPER.writeValueAsString(body))
        } catch (e: JsonProcessingException) {
            httpExchange.fail(e)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(StoreRestApiServiceImpl::class.java)
    }
}
