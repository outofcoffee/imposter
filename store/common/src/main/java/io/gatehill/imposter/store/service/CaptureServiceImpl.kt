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

import com.google.common.base.Strings
import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.plugin.config.capture.CaptureConfig
import io.gatehill.imposter.plugin.config.capture.CaptureConfigHolder
import io.gatehill.imposter.plugin.config.capture.ItemCaptureConfig
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.store.core.Store
import io.gatehill.imposter.store.factory.StoreFactory
import io.gatehill.imposter.store.util.StoreExpressionUtil
import io.gatehill.imposter.store.util.StoreUtil
import io.gatehill.imposter.util.BodyQueryUtil
import io.gatehill.imposter.util.ResourceUtil
import org.apache.logging.log4j.LogManager
import java.util.Objects
import javax.inject.Inject

/**
 * Captures request data into stores.
 *
 * @author Pete Cornish
 */
class CaptureServiceImpl @Inject constructor(
    private val storeFactory: StoreFactory,
    engineLifecycle: EngineLifecycleHooks,
) : EngineLifecycleListener {

    init {
        engineLifecycle.registerListener(this)
    }

    override fun beforeBuildingResponse(httpExchange: HttpExchange, resourceConfig: ResourceConfig?) {
        // immediate captures
        captureItems(resourceConfig, httpExchange, ExchangePhase.REQUEST_RECEIVED)
    }

    override fun afterResponseSent(httpExchange: HttpExchange, resourceConfig: ResourceConfig?) {
        // deferred captures
        captureItems(resourceConfig, httpExchange, ExchangePhase.RESPONSE_SENT)
    }

    private fun captureItems(
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        phaseFilter: ExchangePhase
    ) {
        if (resourceConfig is CaptureConfigHolder) {
            val captureConfig = (resourceConfig as CaptureConfigHolder).captureConfig
            captureConfig?.let {
                captureConfig.filterValues { it.enabled && it.phase == phaseFilter }
                    .forEach { (captureConfigKey: String, itemConfig: ItemCaptureConfig) ->
                        captureItem(captureConfigKey, itemConfig, httpExchange)
                    }
            }
        }
    }

    fun captureItem(
        captureConfigKey: String,
        itemConfig: ItemCaptureConfig,
        httpExchange: HttpExchange
    ) {
        val storeName = determineStoreName(itemConfig, httpExchange)

        val itemName: String? =
            determineItemName(itemConfig, httpExchange, storeName, captureConfigKey)

        // item name may not be set, if dynamic value was null
        itemName?.let {
            val itemValue = captureItemValue(itemConfig, httpExchange, captureConfigKey)
            val store = openCaptureStore(httpExchange, storeName)
            store.save(itemName, itemValue, itemConfig.phase)

        } ?: run {
            LOGGER.warn(
                "Could not capture item: {} into store: {} as dynamic item name resolved to null",
                captureConfigKey,
                storeName
            )
        }
    }

    private fun determineStoreName(
        itemConfig: ItemCaptureConfig,
        httpExchange: HttpExchange
    ): String {
        try {
            return (itemConfig.store?.let { capture<String>(httpExchange, it) }
                ?: StoreService.DEFAULT_CAPTURE_STORE_NAME)

        } catch (e: Exception) {
            throw RuntimeException("Error capturing store name: $itemConfig", e)
        }
    }

    /**
     * Determines the item name, if possible.
     * May return `null` if dynamic value could not be resolved or resolves to `null`.
     */
    private fun determineItemName(
        itemConfig: ItemCaptureConfig,
        httpExchange: HttpExchange,
        storeName: String,
        captureConfigKey: String
    ): String? {
        if (Objects.isNull(itemConfig.key)) {
            LOGGER.debug("Capturing item: {} into store: {}", captureConfigKey, storeName)
            return captureConfigKey

        } else {
            try {
                capture<String?>(httpExchange, itemConfig.key)?.let { itemName ->
                    LOGGER.debug(
                        "Capturing item: $captureConfigKey into store: $storeName with name: $itemName"
                    )
                    return itemName

                } ?: run {
                    LOGGER.trace(
                        "Could not capture item name for: $captureConfigKey as item name resolved to null",
                    )
                    return null
                }

            } catch (e: Exception) {
                throw RuntimeException("Error capturing item name: $captureConfigKey", e)
            }
        }
    }

    private fun captureItemValue(
        itemConfig: ItemCaptureConfig,
        httpExchange: HttpExchange,
        captureConfigKey: String
    ): Any? {
        try {
            return capture(httpExchange, itemConfig)
        } catch (e: Exception) {
            throw RuntimeException("Error capturing item value: $captureConfigKey", e)
        }
    }

    private fun openCaptureStore(httpExchange: HttpExchange, storeName: String): Store {
        return if (StoreUtil.isRequestScopedStore(storeName)) {
            val uniqueRequestId = httpExchange.get<String>(ResourceUtil.RC_REQUEST_ID_KEY)!!
            val requestStoreName = StoreUtil.buildRequestStoreName(uniqueRequestId)
            storeFactory.getStoreByName(requestStoreName, true)
        } else {
            storeFactory.getStoreByName(storeName, false)
        }
    }

    /**
     * Use the [CaptureConfig] to determine the value to capture
     * from the [HttpExchange].
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T> capture(
        httpExchange: HttpExchange,
        captureConfig: CaptureConfig?,
    ): T? {
        if (null == captureConfig) {
            return null
        }

        return if (!Strings.isNullOrEmpty(captureConfig.constValue)) {
            captureConfig.constValue as T?

        } else if (!Strings.isNullOrEmpty(captureConfig.pathParam)) {
            httpExchange.request().pathParam(captureConfig.pathParam!!) as T?

        } else if (!Strings.isNullOrEmpty(captureConfig.queryParam)) {
            httpExchange.request().queryParam(captureConfig.queryParam!!) as T?

        } else if (!Strings.isNullOrEmpty(captureConfig.requestHeader)) {
            httpExchange.request().getHeader(captureConfig.requestHeader!!) as T?

        } else if (!Strings.isNullOrEmpty(captureConfig.requestBody.jsonPath)) {
            BodyQueryUtil.queryRequestBodyJsonPath(
                captureConfig.requestBody.jsonPath!!,
                httpExchange
            ) as T?

        } else if (!Strings.isNullOrEmpty(captureConfig.requestBody.xPath)) {
            BodyQueryUtil.queryRequestBodyXPath(
                captureConfig.requestBody.xPath!!,
                captureConfig.requestBody.xmlNamespaces,
                httpExchange
            ) as T?

        } else if (!Strings.isNullOrEmpty(captureConfig.expression)) {
            StoreExpressionUtil.eval(captureConfig.expression!!, httpExchange) as T?

        } else {
            null
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(CaptureServiceImpl::class.java)
    }
}
