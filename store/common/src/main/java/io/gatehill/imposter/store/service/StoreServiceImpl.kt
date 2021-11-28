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
import com.google.common.base.Strings
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequestHandler
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.capture.CaptureConfig
import io.gatehill.imposter.plugin.config.capture.CaptureConfigHolder
import io.gatehill.imposter.plugin.config.capture.ItemCaptureConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder
import io.gatehill.imposter.plugin.config.system.StoreConfig
import io.gatehill.imposter.plugin.config.system.SystemConfigHolder
import io.gatehill.imposter.script.ExecutionContext
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.store.model.Store
import io.gatehill.imposter.store.model.StoreFactory
import io.gatehill.imposter.store.model.StoreHolder
import io.gatehill.imposter.store.util.StoreUtil
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.MapUtil
import io.gatehill.imposter.util.ResourceUtil
import io.vertx.core.Vertx
import org.apache.commons.text.StringSubstitutor
import org.apache.commons.text.lookup.StringLookupFactory
import org.apache.logging.log4j.LogManager
import java.io.IOException
import java.nio.file.Paths
import java.util.Objects
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class StoreServiceImpl @Inject constructor(
    private val vertx: Vertx,
    private val resourceService: ResourceService,
    private val storeFactory: StoreFactory,
    lifecycleHooks: EngineLifecycleHooks,
) : StoreService, EngineLifecycleListener {

    private var storeItemSubstituter: StringSubstitutor

    init {
        LOGGER.trace("Stores enabled")
        storeItemSubstituter = buildStoreItemSubstituter()
        lifecycleHooks.registerListener(this)
    }

    /**
     * @return a string substituter that replaces placeholders like 'example.foo' with the value of the
     * item 'foo' in the store 'example'.
     */
    private fun buildStoreItemSubstituter(): StringSubstitutor {
        val variableResolver = StringLookupFactory.INSTANCE.functionStringLookup { key: String ->
            try {
                val dotIndex = key.indexOf(".")
                if (dotIndex > 0) {
                    val storeName = key.substring(0, dotIndex)
                    if (storeFactory.hasStoreWithName(storeName)) {
                        val itemKey = key.substring(dotIndex + 1)
                        return@functionStringLookup loadItemFromStore(storeName, itemKey)
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Error replacing template placeholder '$key' with store item", e)
            }
            throw IllegalStateException("Unknown store for template placeholder: $key")
        }
        return StringSubstitutor(variableResolver)
    }

    private fun loadItemFromStore(storeName: String, rawItemKey: String): Any? {
        // check for jsonpath expression
        var itemKey = rawItemKey
        val colonIndex = itemKey.indexOf(":")

        val jsonPath: String?
        if (colonIndex > 0) {
            jsonPath = itemKey.substring(colonIndex + 1)
            itemKey = itemKey.substring(0, colonIndex)
        } else {
            jsonPath = null
        }

        val store = storeFactory.getStoreByName(storeName, false)
        val itemValue = store.load<Any>(itemKey)

        return jsonPath?.let { JSONPATH_PARSE_CONTEXT.parse(itemValue).read(jsonPath) } ?: itemValue
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

    private fun handleLoadAll(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>
    ): HttpRequestHandler {
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
    ): HttpRequestHandler {
        return resourceService.handleRoute(imposterConfig, allPluginConfigs, vertx) { httpExchange: HttpExchange ->
            val storeName = httpExchange.pathParam("storeName")!!
            storeFactory.deleteStoreByName(storeName)
            LOGGER.debug("Deleted store: {}", storeName)

            httpExchange.response()
                .setStatusCode(HttpUtil.HTTP_NO_CONTENT)
                .end()
        }
    }

    private fun handleLoadSingle(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>
    ): HttpRequestHandler {
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
    ): HttpRequestHandler {
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
    ): HttpRequestHandler {
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
    ): HttpRequestHandler {
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

    override fun beforeBuildingResponse(httpExchange: HttpExchange, resourceConfig: ResponseConfigHolder?) {
        if (resourceConfig is CaptureConfigHolder) {
            val captureConfig = (resourceConfig as CaptureConfigHolder).captureConfig
            captureConfig?.let {
                val jsonPathContextHolder = AtomicReference<DocumentContext>()
                captureConfig.forEach { (captureConfigKey: String, itemConfig: ItemCaptureConfig) ->
                    val storeName = itemConfig.store ?: DEFAULT_CAPTURE_STORE_NAME
                    val itemName: String? = determineItemName(httpExchange, jsonPathContextHolder, captureConfigKey, itemConfig, storeName)

                    // itemname may not be set, if dynamic value was null
                    itemName?.let {
                        val itemValue = captureItemValue(httpExchange, jsonPathContextHolder, captureConfigKey, itemConfig)
                        val store = openCaptureStore(httpExchange, storeName)
                        store.save(itemName, itemValue)

                    } ?: run {
                        LOGGER.warn(
                            "Could not capture item: {} into store: {} as dynamic item name resolved to null",
                            captureConfigKey,
                            storeName
                        )
                    }
                }
            }
        }
    }

    /**
     * Determines the item name, if possible.
     * May return `null` if dynamic value could not be resolved or resolves to `null`.
     */
    private fun determineItemName(
        httpExchange: HttpExchange,
        jsonPathContextHolder: AtomicReference<DocumentContext>,
        captureConfigKey: String,
        itemConfig: ItemCaptureConfig,
        storeName: String
    ): String? {
        if (Objects.isNull(itemConfig.key)) {
            LOGGER.debug("Capturing item: {} into store: {}", captureConfigKey, storeName)
            return captureConfigKey

        } else {
            try {
                captureValue<String?>(httpExchange, itemConfig.key, jsonPathContextHolder)?.let { itemName ->
                    LOGGER.debug(
                        "Capturing item: {} into store: {} with dynamic name: {}",
                        captureConfigKey,
                        storeName,
                        itemName
                    )
                    return itemName

                } ?: run {
                    LOGGER.trace(
                        "Could not capture item name for: {} as dynamic item name resolved to null",
                        captureConfigKey,
                    )
                    return null
                }

            } catch (e: Exception) {
                throw RuntimeException("Error capturing item name: $captureConfigKey", e)
            }
        }
    }

    private fun captureItemValue(
        httpExchange: HttpExchange,
        jsonPathContextHolder: AtomicReference<DocumentContext>,
        captureConfigKey: String,
        itemConfig: ItemCaptureConfig
    ): Any? {
        val itemValue = if (!Strings.isNullOrEmpty(itemConfig.constValue)) {
            itemConfig.constValue
        } else {
            try {
                captureValue<Any>(httpExchange, itemConfig, jsonPathContextHolder)
            } catch (e: Exception) {
                throw RuntimeException("Error capturing item value: $captureConfigKey", e)
            }
        }
        return itemValue
    }

    private fun openCaptureStore(httpExchange: HttpExchange, storeName: String): Store {
        val store: Store?
        if (StoreUtil.isRequestScopedStore(storeName)) {
            val uniqueRequestId = httpExchange.get<String>(ResourceUtil.RC_REQUEST_ID_KEY)!!
            val requestStoreName = StoreUtil.buildRequestStoreName(uniqueRequestId)
            store = storeFactory.getStoreByName(requestStoreName, true)
        } else {
            store = storeFactory.getStoreByName(storeName, false)
        }
        return store
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> captureValue(
        httpExchange: HttpExchange,
        itemConfig: CaptureConfig?,
        jsonPathContextHolder: AtomicReference<DocumentContext>
    ): T? {
        return if (!Strings.isNullOrEmpty(itemConfig!!.pathParam)) {
            httpExchange.pathParam(itemConfig.pathParam!!) as T

        } else if (!Strings.isNullOrEmpty(itemConfig.queryParam)) {
            httpExchange.queryParam(itemConfig.queryParam!!) as T

        } else if (!Strings.isNullOrEmpty(itemConfig.requestHeader)) {
            httpExchange.request().getHeader(itemConfig.requestHeader!!) as T

        } else if (!Strings.isNullOrEmpty(itemConfig.jsonPath)) {
            var jsonPathContext = jsonPathContextHolder.get()
            if (Objects.isNull(jsonPathContext)) {
                jsonPathContext = JSONPATH_PARSE_CONTEXT.parse(httpExchange.bodyAsString)
                jsonPathContextHolder.set(jsonPathContext)
            }
            jsonPathContext.read<T>(itemConfig.jsonPath)

        } else {
            null
        }
    }

    override fun beforeBuildingRuntimeContext(
        httpExchange: HttpExchange,
        additionalBindings: MutableMap<String, Any>,
        executionContext: ExecutionContext
    ) {
        val requestId = httpExchange.get<String>(ResourceUtil.RC_REQUEST_ID_KEY)!!
        additionalBindings["stores"] = StoreHolder(storeFactory, requestId)
    }

    override fun beforeTransmittingTemplate(httpExchange: HttpExchange, responseTemplate: String?): String? {
        return responseTemplate?.let {
            // shim for request scoped store
            val uniqueRequestId = httpExchange.get<String>(ResourceUtil.RC_REQUEST_ID_KEY)!!
            val responseData = requestStorePrefixPattern
                .matcher(responseTemplate)
                .replaceAll("\\$\\{" + StoreUtil.buildRequestStoreName(uniqueRequestId) + ".")

            storeItemSubstituter.replace(responseData)
        }
    }

    override fun afterHttpExchangeHandled(httpExchange: HttpExchange) {
        // clean up request store if one exists
        httpExchange.get<String?>(ResourceUtil.RC_REQUEST_ID_KEY)?.let { uniqueRequestId: String ->
            storeFactory.deleteStoreByName(
                StoreUtil.buildRequestStoreName(uniqueRequestId)
            )
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(StoreServiceImpl::class.java)

        /**
         * Default to request scope unless specified.
         */
        private const val DEFAULT_CAPTURE_STORE_NAME: String = StoreUtil.REQUEST_SCOPED_STORE_NAME

        private val JSONPATH_PARSE_CONTEXT = JsonPath.using(
            Configuration.builder()
                .mappingProvider(JacksonMappingProvider())
                .build()
        )

        private val requestStorePrefixPattern = Pattern.compile("\\$\\{" + StoreUtil.REQUEST_SCOPED_STORE_NAME + "\\.")
    }
}
