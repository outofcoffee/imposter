/*
 * Copyright (c) 2023-2023.
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

package io.gatehill.imposter.store.graphql

import com.apurebase.kgraphql.KGraphQL
import com.apurebase.kgraphql.ValidationException
import com.apurebase.kgraphql.schema.Schema
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.store.factory.StoreFactory
import io.gatehill.imposter.store.graphql.model.GraphQLRequest
import io.gatehill.imposter.store.graphql.model.StoreItem
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.MapUtil
import io.gatehill.imposter.util.supervisedDefaultCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import javax.inject.Inject

class GraphQLQueryService @Inject constructor(
    private val storeFactory: StoreFactory,
    engineLifecycle: EngineLifecycleHooks,
) : EngineLifecycleListener, CoroutineScope by supervisedDefaultCoroutineScope {

    private val logger: Logger = LogManager.getLogger(GraphQLQueryService::class.java)
    private val schema: Schema

    init {
        engineLifecycle.registerListener(this)
        schema = buildSchema()
    }

    private fun buildSchema(): Schema {
        return KGraphQL.schema {
            configure {
                useDefaultPrettyPrinter = true
            }

            query("items") {
                resolver { storeName: String, keyPrefix: String? ->
                    val store = storeFactory.getStoreByName(storeName, false)

                    val rawItems: Map<String, Any?> = keyPrefix?.let {
                        store.loadByKeyPrefix(keyPrefix)
                    } ?: run {
                        store.loadAll()
                    }

                    val items = rawItems.entries.map {
                        StoreItem(it.key, it.value.toString())
                    }
                    if (logger.isTraceEnabled) {
                        logger.trace("GraphQL query produced ${items.size} results: {}", items)
                    } else {
                        logger.debug("GraphQL query produced ${items.size} results")
                    }
                    return@resolver items
                }
            }

            // workaround for GraphiQL bug: https://github.com/pgutkowski/KGraphQL/issues/17
            mutation("doNothing") {
                description = "Does nothing"
                resolver { -> "noop" }
            }
        }
    }

    override fun afterRoutesConfigured(
        imposterConfig: ImposterConfig,
        allPluginConfigs: List<PluginConfig>,
        router: HttpRouter
    ) {
        bindQueryRoutes(router)
    }

    private fun bindQueryRoutes(router: HttpRouter) {
        logger.debug("Binding GraphQL routes to $requestPath")

        // see https://graphql.org/learn/serving-over-http/
        router.get(requestPath).handler { httpExchange ->
            val request = httpExchange.request
            val query = request.getQueryParam("query")
            if (query.isNullOrBlank()) {
                httpExchange.response
                    .setStatusCode(400)
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                    .end("No query parameter named 'query' was provided")
                return@handler
            }

            val variables = request.getQueryParam("variables")
            execute(query, variables, httpExchange)
        }

        router.post(requestPath).handler { httpExchange ->
            val httpRequest = httpExchange.request
            val contentLength = httpRequest.getHeader("Content-Length")
            if ((contentLength?.toInt() ?: 0) <= 0) {
                httpExchange.response
                    .setStatusCode(400)
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                    .end("No GraphQL body was was provided")
                return@handler
            }

            val request = MapUtil.JSON_MAPPER.readValue(httpRequest.body!!.bytes, GraphQLRequest::class.java)
            if (logger.isTraceEnabled) {
                logger.trace("Processing GraphQL query: ${request.query} with variables: ${request.variables}")
            }
            execute(request.query, request.variables, httpExchange)
        }
    }

    fun execute(
        query: String,
        variables: String?,
        httpExchange: HttpExchange
    ) = launch {
        try {
            val result = schema.execute(query, variables)

            httpExchange.response
                .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                .end(result)

        } catch (e: ValidationException) {
            logger.error("Invalid GraphQL request", e)
            httpExchange.response
                .setStatusCode(400)
                .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                .end("Invalid GraphQL request: ${e.localizedMessage}")

        } catch (e: Exception) {
            httpExchange.fail(e)
        }
    }

    companion object {
        private const val requestPath = "/system/store/graphql"
    }
}
