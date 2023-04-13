/*
 * Copyright (c) 2016-2023.
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
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.config.util.MetaUtil
import io.gatehill.imposter.http.HttpRoute
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.http.SingletonResourceMatcher
import io.gatehill.imposter.inject.BootstrapModule
import io.gatehill.imposter.inject.EngineModule
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.plugin.PluginDiscoveryStrategy
import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.plugin.PluginManagerImpl
import io.gatehill.imposter.plugin.RoutablePlugin
import io.gatehill.imposter.plugin.config.ConfigurablePlugin
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.server.HttpServer
import io.gatehill.imposter.server.ServerFactory
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.service.security.CorsService
import io.gatehill.imposter.util.AsyncUtil
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.InjectorUtil
import io.gatehill.imposter.util.MetricsUtil
import io.gatehill.imposter.util.supervisedDefaultCoroutineScope
import io.vertx.core.Promise
import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import kotlin.io.path.exists

/**
 * @author Pete Cornish
 */
class Imposter(
    private val vertx: Vertx,
    private val imposterConfig: ImposterConfig,
    private val pluginDiscoveryStrategy: PluginDiscoveryStrategy,
    private val additionalModules: List<Module>,
) : CoroutineScope by supervisedDefaultCoroutineScope {

    private val engineLifecycle = EngineLifecycleHooks()
    private val pluginManager: PluginManager = PluginManagerImpl(pluginDiscoveryStrategy)

    @Inject
    private lateinit var serverFactory: ServerFactory

    @Inject
    private lateinit var resourceService: ResourceService

    @Inject
    private lateinit var corsService: CorsService

    private var httpServer: HttpServer? = null

    private val preferExactMatchRoutes: Boolean
        get() = EnvVars.getEnv("IMPOSTER_PREFER_EXACT_MATCH_ROUTES")?.toBoolean() != false

    fun start(): CompletableFuture<Unit> = future {
        try {
            LOGGER.info("Starting mock engine ${MetaUtil.readVersion()}")

            val plugins = defaultPlugins.toMutableList()
            imposterConfig.plugins?.let(plugins::addAll)

            val pluginConfigs = processConfiguration()
            val dependencies = pluginManager.preparePluginsFromConfig(imposterConfig, plugins, pluginConfigs)

            val allModules = mutableListOf<Module>().apply {
                add(BootstrapModule(vertx, imposterConfig, engineLifecycle, pluginDiscoveryStrategy, pluginManager))
                add(EngineModule())
                addAll(dependencies.flatMap { it.requiredModules })
                addAll(additionalModules)
            }

            val injector = InjectorUtil.create(*allModules.toTypedArray())
            injector.injectMembers(this@Imposter)

            pluginManager.startPlugins(injector, pluginConfigs)

            val router = configureRoutes()
            httpServer = serverFactory.provide(injector, imposterConfig, vertx, router).await()

            LOGGER.info("Mock engine up and running on {}", imposterConfig.serverUrl)

        } catch (e: Exception) {
            engineLifecycle.forEach { listener -> listener.onStartupError(e) }
            throw e
        }
    }

    private fun processConfiguration(): Map<String, List<File>> {
        val configFiles = ConfigUtil.discoverConfigFiles(imposterConfig.configDirs)

        if (EnvVars.discoverEnvFiles) {
            val envFiles = configFiles.map { Paths.get(it.parent, ".env") }.filter { it.exists() }
            if (envFiles.isNotEmpty()) {
                EnvVars.reset(envFiles)
            }
        }

        finaliseEngineConfig()

        return ConfigUtil.loadPluginConfigs(imposterConfig, pluginManager, configFiles)
    }

    private fun finaliseEngineConfig() {
        imposterConfig.serverUrl = HttpUtil.buildServerUrl(imposterConfig).toString()

        EnvVars.getEnv("IMPOSTER_PLUGIN_ARGS")?.let {
            imposterConfig.pluginArgs = it.split(",").map(String::trim).associate { pluginArg ->
                val parts = pluginArg.split("=")
                parts[0] to parts[1].removeSurrounding("\"")
            }
        }
        EnvVars.getEnv("IMPOSTER_EMBEDDED_SCRIPT_ENGINE")?.let {
            imposterConfig.useEmbeddedScriptEngine = it.toBoolean()
        }
        if (LOGGER.isTraceEnabled) {
            LOGGER.trace("Engine config: $imposterConfig")
        }
    }

    private fun configureRoutes(): HttpRouter {
        val router = HttpRouter.router(vertx)
        val resourceMatcher = SingletonResourceMatcher.instance

        router.errorHandler(HttpUtil.HTTP_NOT_FOUND, resourceService.buildNotFoundExceptionHandler())
        router.errorHandler(HttpUtil.HTTP_INTERNAL_ERROR, resourceService.buildUnhandledExceptionHandler())
        router.route().handler(serverFactory.createBodyHttpHandler())

        val plugins = pluginManager.getPlugins()

        val allConfigs: List<PluginConfig> = plugins.filterIsInstance<ConfigurablePlugin<*>>().flatMap { it.configs }
        check(allConfigs.isNotEmpty()) {
            "No plugin configurations were found. Configuration directories [${imposterConfig.configDirs.joinToString()}] must contain one or more valid Imposter configuration files compatible with installed plugins."
        }

        MetricsUtil.doIfMetricsEnabled("add metrics endpoint") {
            LOGGER.trace("Metrics enabled")
            router.route("/system/metrics").handler(
                resourceService.passthroughRoute(
                    imposterConfig,
                    allConfigs,
                    resourceMatcher,
                    serverFactory.createMetricsHandler()
                )
            )
        }

        // status check to indicate when server is up
        router.get("/system/status").handler(
            resourceService.handleRoute(imposterConfig, allConfigs, resourceMatcher) { httpExchange ->
                httpExchange.response
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                    .end(HttpUtil.buildStatusResponse())
            })

        resourceService.handleStaticContent(serverFactory, allConfigs, router)

        plugins.filterIsInstance<RoutablePlugin>().forEach { it.configureRoutes(router) }

        // configure CORS after all routes have been added
        corsService.configure(imposterConfig, allConfigs, router, resourceMatcher)

        // fire post route config hooks
        engineLifecycle.forEach { listener: EngineLifecycleListener ->
            listener.afterRoutesConfigured(imposterConfig, allConfigs, router)
        }

        if (preferExactMatchRoutes) {
            LOGGER.trace("Ordering routes by exact matches first")
            router.routes.sortWith { r1, r2 -> countPlaceholders(r1) - countPlaceholders(r2) }
        }

        return router
    }

    private fun countPlaceholders(route: HttpRoute): Int {
        return route.path?.let { path -> path.count { it == ':' } }
            ?: route.regex?.let { 1000 } // weight regex more than placeholders
            ?: 0
    }

    fun stop(promise: Promise<Void>) {
        LOGGER.info("Stopping mock server on {}:{}", imposterConfig.host, imposterConfig.listenPort)
        httpServer?.close(AsyncUtil.resolvePromiseOnCompletion(promise)) ?: promise.complete()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Imposter::class.java)
        private val defaultPlugins = listOf("js-detector", "store-detector")
    }
}
