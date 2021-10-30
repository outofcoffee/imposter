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

package io.gatehill.imposter.plugin.test;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.config.ConfiguredPlugin;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.gatehill.imposter.service.ResourceService;
import io.gatehill.imposter.service.ResponseService;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Consumer;

import static io.gatehill.imposter.plugin.ScriptedPlugin.scriptHandler;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish
 */
public class TestPluginImpl extends ConfiguredPlugin<TestPluginConfig> {
    private final ImposterConfig imposterConfig;
    private final ResourceService resourceService;
    private final ResponseService responseService;

    @Inject
    public TestPluginImpl(@NotNull Vertx vertx, ImposterConfig imposterConfig, ResourceService resourceService, ResponseService responseService) {
        super(vertx);
        this.imposterConfig = imposterConfig;
        this.resourceService = resourceService;
        this.responseService = responseService;
    }

    @Override
    protected Class<TestPluginConfig> getConfigClass() {
        return TestPluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<? extends TestPluginConfig> configs) {
        // no-op
    }

    @Override
    public void configureRoutes(Router router) {
        getConfigs().forEach(config -> {
            // root resource
            ofNullable(config.getPath()).ifPresent(path -> configureRoute(config, config, router, path));

            // subresources
            ofNullable(config.getResources()).ifPresent(resources -> resources.forEach(resource ->
                    configureRoute(config, resource, router, resource.getPath())
            ));
        });
    }

    private void configureRoute(TestPluginConfig pluginConfig, ResponseConfigHolder resourceConfig, Router router, String path) {
        router.route(path).handler(resourceService.handleRoute(imposterConfig, pluginConfig, getVertx(), routingContext -> {
            final Consumer<ResponseBehaviour> defaultBehaviourHandler = responseBehaviour -> {
                responseService.sendResponse(pluginConfig, resourceConfig, routingContext, responseBehaviour);
            };

            scriptHandler(
                    pluginConfig,
                    resourceConfig,
                    routingContext,
                    getInjector(),
                    defaultBehaviourHandler
            );
        }));
    }
}
