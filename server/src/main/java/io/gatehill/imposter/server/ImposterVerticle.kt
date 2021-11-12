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
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.plugin.config.ConfigurablePlugin
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.scripting.groovy.GroovyScriptingModule
import io.gatehill.imposter.scripting.nashorn.NashornScriptingModule
import io.gatehill.imposter.server.util.FeatureModuleUtil
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.util.AsyncUtil.resolveFutureOnCompletion
import io.gatehill.imposter.util.FeatureUtil.isFeatureEnabled
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.HttpUtil.buildStatusResponse
import io.gatehill.imposter.util.InjectorUtil.injector
import io.gatehill.imposter.util.MetricsUtil
import io.gatehill.imposter.util.MetricsUtil.createHandler
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.impl.BodyHandlerImpl
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

    override fun start(startFuture: Future<Void>) {
        LOGGER.trace("Initialising mock engine")
        vertx.executeBlocking<Nothing>({ future ->
            try {
                startEngine()
                injector!!.injectMembers(this@ImposterVerticle)
                httpServer = serverFactory.provide(imposterConfig, future, vertx, configureRoutes())
            } catch (e: Exception) {
                future.fail(e)
            }
        }) { result ->
            if (result.failed()) {
                startFuture.fail(result.cause())
            } else {
                LOGGER.info("Mock engine up and running on {}", imposterConfig.serverUrl)
                startFuture.complete()
            }
        }
    }

    private fun startEngine() {
        val bootstrapModules: MutableList<Module> = mutableListOf(
            BootstrapModule(vertx, imposterConfig, imposterConfig.serverFactory),
            GroovyScriptingModule(),
            NashornScriptingModule()
        )
        bootstrapModules.addAll(FeatureModuleUtil.discoverFeatureModules())

        val imposter = Imposter(imposterConfig, bootstrapModules.toTypedArray())
        imposter.start()
    }

    override fun stop(stopFuture: Future<Void>) {
        LOGGER.info("Stopping mock server on {}:{}", imposterConfig.host, imposterConfig.listenPort)
        httpServer?.let { server: HttpServer -> server.close(resolveFutureOnCompletion(stopFuture)) }
    }

    private fun configureRoutes(): Router {
        val router = Router.router(vertx)
        router.errorHandler(500, resourceService.buildUnhandledExceptionHandler())
        router.route().handler(BodyHandlerImpl())

        val allConfigs: MutableList<PluginConfig> = mutableListOf()
        pluginManager.getPlugins()
            .filter { p: Plugin -> p is ConfigurablePlugin<*> }
            .forEach { p: Plugin -> allConfigs.addAll((p as ConfigurablePlugin<*>).configs) }

        check(allConfigs.isNotEmpty()) {
            "No plugin configurations were found. The configuration directory must contain one or more valid Imposter configuration files compatible with installed plugins."
        }

        if (isFeatureEnabled(MetricsUtil.FEATURE_NAME_METRICS)) {
            LOGGER.trace("Metrics enabled")
            router.route("/system/metrics").handler(
                resourceService.passthroughRoute(imposterConfig, allConfigs, vertx, createHandler())
            )
        }

        // status check to indicate when server is up
        router["/system/status"].handler(
            resourceService.handleRoute(imposterConfig, allConfigs, vertx) { routingContext ->
                routingContext.response()
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                    .end(buildStatusResponse())
            })

        pluginManager.getPlugins().forEach { plugin: Plugin -> plugin.configureRoutes(router) }

        // fire post route config hooks
        engineLifecycle.forEach { listener: EngineLifecycleListener -> listener.afterRoutesConfigured(imposterConfig, allConfigs, router) }
        return router
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ImposterVerticle::class.java)
    }
}