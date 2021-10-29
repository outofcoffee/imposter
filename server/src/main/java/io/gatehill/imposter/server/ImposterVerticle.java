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

package io.gatehill.imposter.server;

import com.google.inject.Module;
import io.gatehill.imposter.Imposter;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.plugin.config.ConfigurablePlugin;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.scripting.groovy.GroovyScriptingModule;
import io.gatehill.imposter.scripting.nashorn.NashornScriptingModule;
import io.gatehill.imposter.server.util.FeatureModuleUtil;
import io.gatehill.imposter.service.ResourceService;
import io.gatehill.imposter.util.AsyncUtil;
import io.gatehill.imposter.util.FeatureUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.InjectorUtil;
import io.gatehill.imposter.util.MetricsUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.impl.BodyHandlerImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish
 */
public class ImposterVerticle extends AbstractVerticle {
    private static final Logger LOGGER = LogManager.getLogger(ImposterVerticle.class);

    @Inject
    private PluginManager pluginManager;

    @Inject
    private ServerFactory serverFactory;

    @Inject
    private EngineLifecycleHooks engineLifecycle;

    @Inject
    private ResourceService resourceService;

    private final ImposterConfig imposterConfig;

    private HttpServer httpServer;

    public ImposterVerticle() {
        imposterConfig = ConfigHolder.getConfig();
    }

    @Override
    public void start(Future<Void> startFuture) {
        LOGGER.trace("Initialising mock engine");

        vertx.executeBlocking(future -> {
            try {
                startEngine();
                InjectorUtil.getInjector().injectMembers(ImposterVerticle.this);
                httpServer = serverFactory.provide(imposterConfig, future, vertx, configureRoutes());
            } catch (Exception e) {
                future.fail(e);
            }
        }, result -> {
            if (result.failed()) {
                startFuture.fail(result.cause());
            } else {
                LOGGER.info("Mock engine up and running on {}", imposterConfig.getServerUrl());
                startFuture.complete();
            }
        });
    }

    private void startEngine() {
        final List<Module> bootstrapModules = newArrayList(
                new BootstrapModule(vertx, imposterConfig, imposterConfig.getServerFactory()),
                new GroovyScriptingModule(),
                new NashornScriptingModule()
        );
        bootstrapModules.addAll(FeatureModuleUtil.discoverFeatureModules());

        final Imposter imposter = new Imposter(imposterConfig, bootstrapModules);
        imposter.start();
    }

    @Override
    public void stop(Future<Void> stopFuture) {
        LOGGER.info("Stopping mock server on {}:{}", imposterConfig.getHost(), imposterConfig.getListenPort());
        ofNullable(httpServer).ifPresent(server -> server.close(AsyncUtil.resolveFutureOnCompletion(stopFuture)));
    }

    private Router configureRoutes() {
        final Router router = Router.router(vertx);
        router.errorHandler(500, resourceService.buildUnhandledExceptionHandler());
        router.route().handler(new BodyHandlerImpl());

        final List<PluginConfig> allConfigs = new ArrayList<>();
        pluginManager.getPlugins().stream()
                .filter(p -> p instanceof ConfigurablePlugin)
                .forEach(p -> allConfigs.addAll(((ConfigurablePlugin<?>) p).getConfigs()));

        if (allConfigs.isEmpty()) {
            throw new IllegalStateException("No plugin configurations were found. The configuration directory must contain one or more valid Imposter configuration files compatible with installed plugins.");
        }

        if (FeatureUtil.isFeatureEnabled(MetricsUtil.FEATURE_NAME_METRICS)) {
            LOGGER.trace("Metrics enabled");

            router.route("/system/metrics").handler(
                    resourceService.passthroughRoute(imposterConfig, allConfigs, vertx, MetricsUtil.createHandler())
            );
        }

        // status check to indicate when server is up
        router.get("/system/status").handler(resourceService.handleRoute(imposterConfig, allConfigs, vertx, routingContext ->
                routingContext.response()
                        .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                        .end(HttpUtil.buildStatusResponse())
        ));

        pluginManager.getPlugins().forEach(plugin -> plugin.configureRoutes(router));

        // fire post route config hooks
        engineLifecycle.forEach(listener -> listener.afterRoutesConfigured(imposterConfig, allConfigs, router));

        return router;
    }
}
