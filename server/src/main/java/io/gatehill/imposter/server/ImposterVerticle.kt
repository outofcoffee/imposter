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
package io.gatehill.imposter.server

import com.google.inject.Module
import io.gatehill.imposter.Imposter
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.plugin.RoutablePlugin
import io.gatehill.imposter.plugin.config.ConfigurablePlugin
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.scripting.common.CommonScriptingModule
import io.gatehill.imposter.scripting.groovy.GroovyScriptingModule
import io.gatehill.imposter.server.util.FeatureModuleUtil
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.util.AsyncUtil.resolvePromiseOnCompletion
import io.gatehill.imposter.util.FeatureUtil.isFeatureEnabled
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.HttpUtil.buildStatusResponse
import io.gatehill.imposter.util.InjectorUtil.injector
import io.gatehill.imposter.util.MetricsUtil
import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import org.apache.logging.log4j.LogManager
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class ImposterVerticle : AbstractVerticle() {
    @Inject
    private lateinit var pluginManager: PluginManager

    @Inject
    private lateinit var serverFactory: ServerFactory

    @Inject
    private lateinit var engineLifecycle: EngineLifecycleHooks

    @Inject
    private lateinit var resourceService: ResourceService

    private val imposterConfig = ConfigHolder.config
    private var httpServer: HttpServer? = null

    override fun start(startPromise: Promise<Void>) {
        LOGGER.trace("Initialising mock engine")
        vertx.executeBlocking<Unit>({ promise ->
            try {
                startEngine()
                injector!!.injectMembers(this@ImposterVerticle)
                httpServer = serverFactory.provide(imposterConfig, promise, vertx, configureRoutes())
            } catch (e: Exception) {
                promise.fail(e)
            }
        }) { result ->
            if (result.failed()) {
                startPromise.fail(result.cause())
            } else {
                LOGGER.info("Mock engine up and running on {}", imposterConfig.serverUrl)
                startPromise.complete()
            }
        }
    }

    private fun startEngine() {
        val bootstrapModules = mutableListOf<Module>(
            BootstrapModule(vertx, imposterConfig, imposterConfig.serverFactory!!),
            GroovyScriptingModule(),
            CommonScriptingModule(),
        )
        bootstrapModules.addAll(FeatureModuleUtil.discoverFeatureModules())

        val imposter = Imposter(imposterConfig, bootstrapModules.toTypedArray())
        imposter.start()
    }

    override fun stop(stopPromise: Promise<Void>) {
        LOGGER.info("Stopping mock server on {}:{}", imposterConfig.host, imposterConfig.listenPort)
        httpServer?.let { server: HttpServer -> server.close(resolvePromiseOnCompletion(stopPromise)) }
    }

    private fun configureRoutes(): HttpRouter {
        val router = HttpRouter.router(vertx)

        router.errorHandler(HttpUtil.HTTP_NOT_FOUND, resourceService.buildNotFoundExceptionHandler())
        router.errorHandler(HttpUtil.HTTP_INTERNAL_ERROR, resourceService.buildUnhandledExceptionHandler())
        router.route().handler(serverFactory.createBodyHttpHandler())

        val allConfigs: List<PluginConfig> = pluginManager.getPlugins()
            .filter { p: Plugin -> p is ConfigurablePlugin<*> }
            .flatMap { p: Plugin -> (p as ConfigurablePlugin<*>).configs }

        check(allConfigs.isNotEmpty()) {
            "No plugin configurations were found. Configuration directories [${imposterConfig.configDirs.joinToString()}] must contain one or more valid Imposter configuration files compatible with installed plugins."
        }

        if (isFeatureEnabled(MetricsUtil.FEATURE_NAME_METRICS)) {
            LOGGER.trace("Metrics enabled")
            router.route("/system/metrics").handler(
                resourceService.passthroughRoute(
                    imposterConfig,
                    allConfigs,
                    vertx,
                    serverFactory.createMetricsHandler()
                )
            )
        }

        // status check to indicate when server is up
        router.get("/system/status").handler(
            resourceService.handleRoute(imposterConfig, allConfigs, vertx) { httpExchange ->
                httpExchange.response()
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                    .end(buildStatusResponse())
            })

        pluginManager.getPlugins().filterIsInstance<RoutablePlugin>().forEach { plugin ->
            plugin.configureRoutes(router)
        }

        // fire post route config hooks
        engineLifecycle.forEach { listener: EngineLifecycleListener ->
            listener.afterRoutesConfigured(imposterConfig, allConfigs, router)
        }
        return router
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ImposterVerticle::class.java)
    }
}
