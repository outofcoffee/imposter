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

package io.gatehill.imposter.plugin;

import com.google.inject.Injector;
import io.gatehill.imposter.http.DefaultResponseBehaviourFactory;
import io.gatehill.imposter.http.DefaultStatusCodeFactory;
import io.gatehill.imposter.http.ResponseBehaviourFactory;
import io.gatehill.imposter.http.StatusCodeFactory;
import io.gatehill.imposter.plugin.config.PluginConfigImpl;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.gatehill.imposter.script.ResponseBehaviourType;
import io.gatehill.imposter.service.ResponseService;
import io.vertx.ext.web.RoutingContext;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ScriptedPlugin<C extends PluginConfigImpl> {
    default void scriptHandler(
            C pluginConfig,
            RoutingContext routingContext,
            Injector injector,
            Consumer<ResponseBehaviour> defaultBehaviourHandler
    ) {
        scriptHandler(
                pluginConfig,
                pluginConfig,
                routingContext,
                injector,
                null,
                DefaultStatusCodeFactory.getInstance(),
                DefaultResponseBehaviourFactory.getInstance(),
                defaultBehaviourHandler
        );
    }

    default void scriptHandler(
            C pluginConfig,
            ResponseConfigHolder resourceConfig,
            RoutingContext routingContext,
            Injector injector,
            Consumer<ResponseBehaviour> defaultBehaviourHandler
    ) {
        scriptHandler(
                pluginConfig,
                resourceConfig,
                routingContext,
                injector,
                null,
                DefaultStatusCodeFactory.getInstance(),
                DefaultResponseBehaviourFactory.getInstance(),
                defaultBehaviourHandler
        );
    }

    default void scriptHandler(
            C pluginConfig,
            RoutingContext routingContext,
            Injector injector,
            Map<String, Object> additionalContext,
            Consumer<ResponseBehaviour> defaultBehaviourHandler
    ) {
        scriptHandler(
                pluginConfig,
                pluginConfig,
                routingContext,
                injector,
                additionalContext,
                DefaultStatusCodeFactory.getInstance(),
                DefaultResponseBehaviourFactory.getInstance(),
                defaultBehaviourHandler
        );
    }

    default void scriptHandler(
            C pluginConfig,
            ResponseConfigHolder resourceConfig,
            RoutingContext routingContext,
            Injector injector,
            Map<String, Object> additionalContext,
            StatusCodeFactory statusCodeFactory,
            ResponseBehaviourFactory responseBehaviourFactory,
            Consumer<ResponseBehaviour> defaultBehaviourHandler
    ) {
        final ResponseService responseService = injector.getInstance(ResponseService.class);
        responseService.handle(
                pluginConfig,
                resourceConfig,
                routingContext,
                injector,
                additionalContext,
                statusCodeFactory,
                responseBehaviourFactory,
                defaultBehaviourHandler
        );
    }
}
