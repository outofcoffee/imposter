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
package io.gatehill.imposter.plugin.test

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.http.SingletonResourceMatcher
import io.gatehill.imposter.plugin.config.ConfiguredPlugin
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.service.ResourceService
import io.gatehill.imposter.service.ResponseRoutingService
import io.gatehill.imposter.service.ResponseService
import io.vertx.core.Vertx
import javax.inject.Inject

/**
 * @author Pete Cornish
 */
class TestPluginImpl @Inject constructor(
    vertx: Vertx,
    imposterConfig: ImposterConfig,
    private val resourceService: ResourceService,
    private val responseService: ResponseService,
    private val responseRoutingService: ResponseRoutingService,
) : ConfiguredPlugin<TestPluginConfig>(vertx, imposterConfig) {

    override val configClass = TestPluginConfig::class.java

    private val resourceMatcher = SingletonResourceMatcher.instance

    override fun configureRoutes(router: HttpRouter) {
        configs.forEach { config: TestPluginConfig ->
            // root resource
            config.path?.let { path: String -> configureRoute(config, config, router, path) }

            // subresources
            config.resources?.forEach { resource: TestPluginResourceConfig ->
                configureRoute(config, resource, router, resource.path!!)
            }
        }
    }

    private fun configureRoute(
        pluginConfig: TestPluginConfig,
        resourceConfig: BasicResourceConfig,
        router: HttpRouter,
        path: String
    ) {
        router.route(path).handler(resourceService.handleRoute(imposterConfig, pluginConfig, vertx, resourceMatcher) { httpExchange ->
            val defaultBehaviourHandler = { responseBehaviour: ResponseBehaviour ->
                responseService.sendResponse(
                    pluginConfig,
                    resourceConfig,
                    httpExchange,
                    responseBehaviour
                )
            }
            responseRoutingService.route(
                pluginConfig,
                resourceConfig,
                httpExchange,
                defaultBehaviourHandler
            )
        })
    }
}
