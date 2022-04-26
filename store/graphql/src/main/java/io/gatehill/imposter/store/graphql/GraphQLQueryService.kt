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
            val query = httpExchange.queryParam("query")
            if (query.isNullOrBlank()) {
                httpExchange.response()
                    .setStatusCode(400)
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                    .end("No query parameter named 'query' was provided")
                return@handler
            }

            val variables = httpExchange.queryParam("variables")
            execute(query, variables, httpExchange)
        }

        router.post(requestPath).handler { httpExchange ->
            val contentLength = httpExchange.request().getHeader("Content-Length")
            if ((contentLength?.toInt() ?: 0) <= 0) {
                httpExchange.response()
                    .setStatusCode(400)
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                    .end("No GraphQL body was was provided")
                return@handler
            }

            val request = MapUtil.JSON_MAPPER.readValue(httpExchange.body!!.bytes, GraphQLRequest::class.java)
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

            httpExchange.response()
                .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                .end(result)

        } catch (e: ValidationException) {
            logger.error("Invalid GraphQL request", e)
            httpExchange.response()
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
