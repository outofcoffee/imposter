/*
 * Copyright (c) 2016-2024.
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
package io.gatehill.imposter.plugin.sfdc

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpExchangeFutureHandler
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.http.SingletonResourceMatcher
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.config.ConfiguredPlugin
import io.gatehill.imposter.plugin.sfdc.config.SfdcPluginConfig
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.service.ResponseFileService
import io.gatehill.imposter.service.ResponseRoutingService
import io.gatehill.imposter.util.FileUtil.findRow
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.HttpUtil.CONTENT_TYPE
import io.gatehill.imposter.util.HttpUtil.CONTENT_TYPE_JSON
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import org.apache.logging.log4j.LogManager
import java.util.StringTokenizer
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

/**
 * Plugin for SFDC.
 *
 * @author Pete Cornish
 */
@PluginInfo("sfdc")
class SfdcPluginImpl @Inject constructor(
    vertx: Vertx,
    imposterConfig: ImposterConfig,
    private val resourceService: ResourceService,
    private val responseFileService: ResponseFileService,
    private val responseRoutingService: ResponseRoutingService,
) : ConfiguredPlugin<SfdcPluginConfig>(
    vertx, imposterConfig
) {
    override val configClass = SfdcPluginConfig::class.java

    private val resourceMatcher = SingletonResourceMatcher.instance

    override fun configureRoutes(router: HttpRouter) {
        // oauth handler
        router.post("/services/oauth2/token").handler(
            resourceService.handleRoute(imposterConfig, configs, resourceMatcher) { httpExchange: HttpExchange ->
                LOGGER.info("Handling oauth request: {}", httpExchange.request.bodyAsString)
                val authResponse = JsonObject()
                authResponse.put("access_token", "dummyAccessToken")
                authResponse.put("instance_url", imposterConfig.serverUrl)
                httpExchange.response.putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                httpExchange.response.end(authResponse.encode())
            }
        )

        // query handler
        router.get("/services/data/:apiVersion/query/").handler(
            resourceService.handleRoute(imposterConfig, configs, resourceMatcher) { httpExchange: HttpExchange ->
                val request = httpExchange.request
                val apiVersion = request.getPathParam("apiVersion")!!

                // e.g. 'SELECT Name, Id from Account LIMIT 100'
                val query = request.getQueryParam("q")!!
                val sObjectName = getSObjectName(query)
                    ?: throw RuntimeException("Could not determine SObject name from query: $query")

                val config = configs.firstOrNull { sfdcPluginConfig: SfdcPluginConfig ->
                    sObjectName.equals(sfdcPluginConfig.sObjectName, ignoreCase = true)
                } ?: throw RuntimeException("Unable to find mock config for SObject: $sObjectName")

                // script should fire first
                responseRoutingService.route(config, httpExchange) { responseBehaviour ->
                    // enrich records
                    val records = responseFileService.loadResponseAsJsonArray(config, responseBehaviour)
                    for (i in 0 until records.size()) {
                        addRecordAttributes(records.getJsonObject(i), apiVersion, config.sObjectName)
                    }

                    val responseWrapper = JsonObject()
                    responseWrapper.put("done", true)
                    responseWrapper.put("records", records)
                    responseWrapper.put("totalSize", records.size())
                    LOGGER.info("Sending {} SObjects in response to query: {}", records.size(), query)

                    httpExchange.response
                        .putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                        .setStatusCode(HttpUtil.HTTP_OK)
                        .end(Buffer.buffer(responseWrapper.encodePrettily()))
                }
            }
        )

        // get SObject handler
        configs.forEach { config: SfdcPluginConfig ->
            val handler = resourceService.handleRoute(imposterConfig, config, resourceMatcher) { httpExchange: HttpExchange ->
                // script should fire first
                responseRoutingService.route(config, httpExchange) { responseBehaviour ->
                    val request = httpExchange.request
                    val apiVersion = request.getPathParam("apiVersion")!!
                    val sObjectId = request.getPathParam("sObjectId")

                    // find and enrich record
                    val result = findRow(
                        idFieldName = FIELD_ID,
                        rowId = sObjectId,
                        rows = responseFileService.loadResponseAsJsonArray(config, responseBehaviour)
                    )?.let { r: JsonObject -> addRecordAttributes(r, apiVersion, config.sObjectName) }

                    val response = httpExchange.response

                    result?.let {
                        LOGGER.info("Sending SObject with ID: {}", sObjectId)
                        response.putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                            .setStatusCode(HttpUtil.HTTP_OK)
                            .end(Buffer.buffer(result.encodePrettily()))
                    } ?: run {
                        // no such record
                        LOGGER.error("{} SObject with ID: {} not found", config.sObjectName, sObjectId)
                        response.setStatusCode(HttpUtil.HTTP_NOT_FOUND).end()
                    }
                }
            }
            router.get("/services/data/:apiVersion/sobjects/${config.sObjectName}/:sObjectId").handler(handler)
        }

        // create SObject handler
        router.post("/services/data/:apiVersion/sobjects/:sObjectName").handler(
            resourceService.handleRoute(imposterConfig, configs, resourceMatcher) { httpExchange: HttpExchange ->
                val request = httpExchange.request
                val sObjectName = request.getPathParam("sObjectName")
                val sObject = request.bodyAsJson
                LOGGER.info("Received create request for {}: {}", sObjectName, sObject)
                val result = JsonObject()

                // Note: ID response field name has to be lowercase, for some reason
                result.put("id", generateBase62Id())
                result.put("success", true)
                httpExchange.response
                    .putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                    .setStatusCode(HttpUtil.HTTP_CREATED)
                    .end(Buffer.buffer(result.encodePrettily()))
            }
        )

        // update SObject handlers
        router.patch("/services/data/:apiVersion/sobjects/:sObjectName/:sObjectId").handler(handleUpdateRequest())
        router.post("/services/data/:apiVersion/sobjects/:sObjectName/:sObjectId").handler(handleUpdateRequest())
    }

    /**
     * Can be a PATCH or a POST request (with query parameter '_HttpMethod=PATCH').
     *
     * @return
     */
    private fun handleUpdateRequest(): HttpExchangeFutureHandler {
        return resourceService.handleRoute(imposterConfig, configs, resourceMatcher) { httpExchange: HttpExchange ->
            val request = httpExchange.request
            val sObjectName = request.getPathParam("sObjectName")
            val sObjectId = request.getPathParam("sObjectId")
            val sObject = request.bodyAsJson

            // SFDC work-around for HTTP clients that don't support PATCH
            if (HttpMethod.PATCH != request.method
                && "PATCH" != request.getQueryParam("_HttpMethod")
            ) {
                httpExchange.fail(HttpUtil.HTTP_BAD_METHOD)
                return@handleRoute
            }
            LOGGER.info("Received update request for {} with ID: {}: {}", sObjectName, sObjectId, sObject)
            httpExchange.response
                .putHeader(CONTENT_TYPE, CONTENT_TYPE_JSON)
                .setStatusCode(HttpUtil.HTTP_NO_CONTENT)
                .end()
        }
    }

    private fun generateBase62Id(): String {
        val characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var b10 = abs(UUID.randomUUID().hashCode()).toLong()
        var ret = ""
        while (b10 > 0) {
            ret = characters[(b10 % 62).toInt()].toString() + ret
            b10 /= 62
        }
        return ret
    }

    private fun getSObjectName(query: String): String? {
        val tokenizer = StringTokenizer(query, " ")
        var token = tokenizer.nextToken()
        while (tokenizer.hasMoreTokens()) {
            if ("FROM".equals(token, ignoreCase = true) && tokenizer.hasMoreTokens()) {
                return tokenizer.nextToken()
            }
            token = tokenizer.nextToken()
        }
        return null
    }

    private fun addRecordAttributes(record: JsonObject, apiVersion: String, sObjectName: String?): JsonObject {
        val sObjectId = record.getString(FIELD_ID)
            ?: throw RuntimeException(String.format("Record missing '%s' field: %s", FIELD_ID, record))

        val attributes = JsonObject()
        attributes.put("type", sObjectName)
        attributes.put("url", "/services/data/$apiVersion/sobjects/$sObjectName/$sObjectId")
        record.put("attributes", attributes)
        return record
    }

    companion object {
        private val LOGGER = LogManager.getLogger(SfdcPluginImpl::class.java)
        private const val FIELD_ID = "Id"
    }
}
