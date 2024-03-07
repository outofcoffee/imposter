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
package io.gatehill.imposter.plugin.hbase

import com.google.common.base.Objects
import com.google.common.base.Strings
import com.google.inject.Key
import com.google.inject.name.Names
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.http.SingletonResourceMatcher
import io.gatehill.imposter.plugin.PluginInfo
import io.gatehill.imposter.plugin.RequireModules
import io.gatehill.imposter.plugin.config.ConfiguredPlugin
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.hbase.config.HBasePluginConfig
import io.gatehill.imposter.plugin.hbase.model.RecordInfo
import io.gatehill.imposter.plugin.hbase.model.ResponsePhase
import io.gatehill.imposter.plugin.hbase.service.ScannerService
import io.gatehill.imposter.plugin.hbase.service.serialisation.DeserialisationService
import io.gatehill.imposter.plugin.hbase.service.serialisation.SerialisationService
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.service.ResponseFileService
import io.gatehill.imposter.service.ResponseRoutingService
import io.gatehill.imposter.util.FileUtil.findRow
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.HttpUtil.CONTENT_TYPE_JSON
import io.gatehill.imposter.util.HttpUtil.readAcceptedContentTypes
import io.gatehill.imposter.util.InjectorUtil
import io.gatehill.imposter.util.completedUnitFuture
import io.gatehill.imposter.util.makeFuture
import io.vertx.core.Vertx
import org.apache.logging.log4j.LogManager
import javax.inject.Inject

/**
 * Plugin for HBase.
 *
 * @author Pete Cornish
 */
@PluginInfo("hbase")
@RequireModules(HBasePluginModule::class)
class HBasePluginImpl @Inject constructor(
    vertx: Vertx,
    imposterConfig: ImposterConfig,
    private val resourceService: ResourceService,
    private val responseFileService: ResponseFileService,
    private val responseRoutingService: ResponseRoutingService,
    private val scannerService: ScannerService,
) : ConfiguredPlugin<HBasePluginConfig>(
    vertx, imposterConfig
) {
    override val configClass = HBasePluginConfig::class.java
    private var tables: Map<String, HBasePluginConfig>? = null

    private val tableConfigs
        get() = tables!!

    private val injector = InjectorUtil.injector!!

    private val resourceMatcher = SingletonResourceMatcher.instance

    override fun configurePlugin(configs: List<HBasePluginConfig>) {
        tables = configs.associateBy { it.tableName }
    }

    override fun configureRoutes(router: HttpRouter) {
        // add route for each distinct path
        tableConfigs.values.map { config: HBasePluginConfig -> ConfigAndPath(config, config.path ?: "") }
            .distinct()
            .forEach { configAndPath: ConfigAndPath ->
                LOGGER.debug("Adding routes for base path: {}", { configAndPath.path.ifEmpty { "<empty>" } })

                // endpoint to allow individual row retrieval
                addRowRetrievalRoute(configAndPath.config, router, configAndPath.path)

                // Note: when scanning for results, the first call obtains a scanner:
                addCreateScannerRoute(configAndPath.config, router, configAndPath.path)
                // ...and the second call returns the results
                addReadScannerResultsRoute(configAndPath.config, router, configAndPath.path)
            }
    }

    /**
     * Handles a request for a particular row within a table.
     *
     * @param pluginConfig
     * @param router
     * @param path
     */
    private fun addRowRetrievalRoute(pluginConfig: PluginConfig, router: HttpRouter, path: String) {
        router.get("$path/:tableName/:recordId/").handler(
            resourceService.handleRoute(imposterConfig, pluginConfig, resourceMatcher) { httpExchange: HttpExchange ->
                val request = httpExchange.request
                val tableName = request.getPathParam("tableName")!!
                val recordId = request.getPathParam("recordId")!!

                val recordInfo = RecordInfo(recordId)
                val config: HBasePluginConfig

                // check that the table is registered
                if (!tableConfigs.containsKey(tableName)) {
                    LOGGER.error("Received row request for unknown table: {}", tableName)
                    httpExchange.response
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .end()
                    return@handleRoute completedUnitFuture()
                } else {
                    LOGGER.info("Received request for row with ID: {} for table: {}", recordId, tableName)
                    config = tableConfigs[tableName]!!
                }

                // script should fire first
                val bindings = buildScriptBindings(
                    ResponsePhase.RECORD,
                    tableName,
                    recordInfo,
                    scannerFilterPrefix = null
                )
                responseRoutingService.route(config, httpExchange, bindings) { responseBehaviour ->
                    makeFuture {
                        // find the right row from results
                        val results = responseFileService.loadResponseAsJsonArray(config, responseBehaviour)
                        val result = findRow(config.idField, recordInfo.recordId, results)
                        val response = httpExchange.response

                        result?.let {
                            val serialiser = findSerialiser(httpExchange)
                            val buffer = serialiser.serialise(tableName, recordInfo.recordId, result)
                            response.setStatusCode(HttpUtil.HTTP_OK).end(buffer)
                        } ?: run {
                            // no such record
                            LOGGER.error("No row found with ID: {} for table: {}", recordInfo.recordId, tableName)
                            response.setStatusCode(HttpUtil.HTTP_NOT_FOUND).end()
                        }
                    }
                }
            })
    }

    /**
     * Handles the first part of a request for results - creation of a scanner. Results are read from the scanner
     * in the handler [addReadScannerResultsRoute].
     *
     * @param pluginConfig
     * @param router
     * @param path
     */
    private fun addCreateScannerRoute(pluginConfig: PluginConfig, router: HttpRouter, path: String) {
        router.post("$path/:tableName/scanner").handler(
            resourceService.handleRoute(imposterConfig, pluginConfig, resourceMatcher) { httpExchange: HttpExchange ->
                val tableName = httpExchange.request.getPathParam("tableName")!!

                // check that the table is registered
                if (!tableConfigs.containsKey(tableName)) {
                    LOGGER.error("Received scanner request for unknown table: {}", tableName)
                    httpExchange.response
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .end()
                    return@handleRoute completedUnitFuture()
                }
                LOGGER.info("Received scanner request for table: {}", tableName)
                val config = tableConfigs[tableName]!!

                val deserialiser = findDeserialiser(httpExchange)
                val scanner = try {
                    deserialiser.decodeScanner(httpExchange)
                } catch (e: Exception) {
                    httpExchange.fail(e)
                    return@handleRoute completedUnitFuture()
                }
                val scannerFilterPrefix = deserialiser.decodeScannerFilterPrefix(scanner)
                LOGGER.debug(
                    "Scanner filter for table: {}: {}, with prefix: {}",
                    tableName,
                    scanner.filter,
                    scannerFilterPrefix
                )

                // assert prefix matches if present
                if (config.prefix?.let { it == scannerFilterPrefix } != false) {
                    LOGGER.info("Scanner filter prefix matches expected value: {}", config.prefix)
                } else {
                    LOGGER.error(
                        "Scanner filter prefix '{}' does not match expected value: {}}",
                        scannerFilterPrefix, config.prefix
                    )
                    httpExchange.fail(HttpUtil.HTTP_INTERNAL_ERROR)
                    return@handleRoute completedUnitFuture()
                }

                // script should fire first
                val bindings = buildScriptBindings(ResponsePhase.SCANNER, tableName, null, scannerFilterPrefix)
                responseRoutingService.route(config, httpExchange, bindings) {
                    makeFuture {
                        val scannerId = scannerService.registerScanner(config, scanner)
                        val resultUrl = imposterConfig.serverUrl + path + "/" + tableName + "/scanner/" + scannerId
                        httpExchange.response
                            .putHeader("Location", resultUrl)
                            .setStatusCode(HttpUtil.HTTP_CREATED)
                            .end()
                    }
                }
            })
    }

    /**
     * Handles the second part of a request for results - reading rows from the scanner created
     * in [addCreateScannerRoute].
     *
     * @param pluginConfig
     * @param router
     * @param path
     */
    private fun addReadScannerResultsRoute(pluginConfig: HBasePluginConfig, router: HttpRouter, path: String) {
        router.get("$path/:tableName/scanner/:scannerId").handler(
            resourceService.handleRoute(imposterConfig, pluginConfig, resourceMatcher) { httpExchange: HttpExchange ->
                val request = httpExchange.request
                val tableName = request.getPathParam("tableName")!!
                val scannerId = request.getPathParam("scannerId")!!

                // query param e.g. ?n=1
                val rows = Integer.valueOf(request.getQueryParam("n"))

                // check that the table is registered
                if (!tableConfigs.containsKey(tableName)) {
                    LOGGER.error("Received result request for unknown table: {}", tableName)
                    httpExchange.response
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .end()
                    return@handleRoute completedUnitFuture()
                }

                // check that the scanner was created
                val scanner = scannerService.fetchScanner(Integer.valueOf(scannerId))
                scanner ?: run {
                    LOGGER.error(
                        "Received result request for nonexistent scanner {} for table: {}",
                        scannerId,
                        tableName
                    )
                    httpExchange.response
                        .setStatusCode(HttpUtil.HTTP_NOT_FOUND)
                        .end()
                    return@handleRoute completedUnitFuture()
                }
                LOGGER.info(
                    "Received result request for {} rows from scanner {} for table: {}",
                    rows,
                    scannerId,
                    tableName
                )

                // load result
                val config = tableConfigs[tableName]!!

                // script should fire first
                val deserialiser = findDeserialiser(httpExchange)
                val bindings = buildScriptBindings(
                    responsePhase = ResponsePhase.RESULTS,
                    tableName = tableName,
                    scannerFilterPrefix = deserialiser.decodeScannerFilterPrefix(scanner.scanner)
                )
                responseRoutingService.route(config, httpExchange, bindings) { responseBehaviour ->
                    makeFuture {
                        // build results
                        val results = responseFileService.loadResponseAsJsonArray(config, responseBehaviour)
                        val serialiser = findSerialiser(httpExchange)
                        val buffer = serialiser.serialise(tableName, scannerId, results, scanner, rows)
                        httpExchange.response
                            .setStatusCode(HttpUtil.HTTP_OK)
                            .end(buffer)
                    }
                }
            })
    }

    /**
     * Find the serialiser binding based on the content types accepted by the client.
     *
     * @param httpExchange the HTTP exchange
     * @return the serialiser
     */
    private fun findSerialiser(httpExchange: HttpExchange): SerialisationService {
        val acceptedContentTypes = readAcceptedContentTypes(httpExchange).toMutableList()

        // add as default to end of the list
        if (!acceptedContentTypes.contains(CONTENT_TYPE_JSON)) {
            acceptedContentTypes += CONTENT_TYPE_JSON
        }

        // search the ordered list
        for (contentType in acceptedContentTypes) {
            try {
                val serialiser = injector.getInstance(
                    Key.get(SerialisationService::class.java, Names.named(contentType))
                )
                LOGGER.debug(
                    "Found serialiser binding {} for content type '{}'",
                    serialiser.javaClass.simpleName,
                    contentType
                )
                return serialiser
            } catch (e: Exception) {
                LOGGER.trace("Unable to load serialiser binding for content type '{}'", contentType, e)
            }
        }
        throw RuntimeException(
            "Unable to find serialiser matching any accepted content type: $acceptedContentTypes"
        )
    }

    /**
     * Find the deserialiser binding based on the content types sent by the client.
     *
     * @param httpExchange the HTTP exchange
     * @return the deserialiser
     */
    private fun findDeserialiser(httpExchange: HttpExchange): DeserialisationService {
        var contentType = httpExchange.request.getHeader("Content-Type")

        // use JSON as default
        if (Strings.isNullOrEmpty(contentType)) {
            contentType = CONTENT_TYPE_JSON
        }
        try {
            val deserialiser = injector.getInstance(
                Key.get(DeserialisationService::class.java, Names.named(contentType))
            )
            LOGGER.debug(
                "Found deserialiser binding {} for content type '{}'",
                deserialiser.javaClass.simpleName,
                contentType
            )
            return deserialiser
        } catch (e: Exception) {
            LOGGER.trace("Unable to load deserialiser binding for content type '{}'", contentType, e)
        }
        throw RuntimeException("Unable to find deserialiser matching content type: $contentType")
    }

    /**
     * Add additional script bindings.
     *
     * @param responsePhase
     * @param tableName
     * @param scannerFilterPrefix
     * @return
     */
    private fun buildScriptBindings(
        responsePhase: ResponsePhase,
        tableName: String,
        recordInfo: RecordInfo? = null,
        scannerFilterPrefix: String?
    ): Map<String, Any> {
        val bindings: MutableMap<String, Any> = mutableMapOf()
        bindings["tableName"] = tableName
        bindings["responsePhase"] = responsePhase
        bindings["scannerFilterPrefix"] = scannerFilterPrefix ?: ""
        recordInfo?.let { bindings["recordInfo"] = recordInfo }
        return bindings
    }

    private class ConfigAndPath(val config: HBasePluginConfig, val path: String) {
        /**
         * Only path is used for equality/hash code.
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || javaClass != other.javaClass) return false
            val that = other as ConfigAndPath
            return Objects.equal(path, that.path)
        }

        /**
         * Only path is used for equality/hash code.
         */
        override fun hashCode(): Int {
            return Objects.hashCode(path)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(HBasePluginImpl::class.java)
    }
}