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
package io.gatehill.imposter.service

import io.gatehill.imposter.exception.ResponseException
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.ResponseBehaviourFactory
import io.gatehill.imposter.http.StatusCodeFactory
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.lifecycle.EngineLifecycleListener
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.ResourcesHolder
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.script.ResponseBehaviourType
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.makeFuture
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

/**
 * Main entrypoint for response routing.
 *
 * @author Pete Cornish
 */
class ResponseRoutingServiceImpl @Inject constructor(
    private val engineLifecycle: EngineLifecycleHooks,
    private val stepService: StepService,
    private val responseService: ResponseService,
) : ResponseRoutingService {
    private val logger = LogManager.getLogger(ResponseRoutingServiceImpl::class.java)

    /**
     * {@inheritDoc}
     */
    override fun route(
        pluginConfig: PluginConfig,
        resourceConfig: BasicResourceConfig?,
        httpExchange: HttpExchange,
        additionalContext: Map<String, Any>?,
        statusCodeFactory: StatusCodeFactory,
        responseBehaviourFactory: ResponseBehaviourFactory,
        defaultBehaviourHandler: DefaultBehaviourHandler,
    ): CompletableFuture<Unit> {
        try {
            engineLifecycle.forEach { listener: EngineLifecycleListener ->
                listener.beforeBuildingResponse(httpExchange, resourceConfig)
            }
            val responseBehaviour = buildResponseBehaviour(
                httpExchange,
                pluginConfig,
                resourceConfig,
                additionalContext,
                statusCodeFactory,
                responseBehaviourFactory
            )
            if (ResponseBehaviourType.SHORT_CIRCUIT == responseBehaviour.behaviourType) {
                return responseService.sendResponse(
                    pluginConfig,
                    resourceConfig,
                    httpExchange,
                    responseBehaviour,
                )
            } else {
                // default behaviour
                return defaultBehaviourHandler(responseBehaviour)
            }
        } catch (e: Exception) {
            val msg = "Error sending mock response for ${LogUtil.describeRequest(httpExchange)}"
            logger.error(msg, e)
            return makeFuture {
                httpExchange.fail(ResponseException(msg, e))
            }
        }
    }

    private fun buildResponseBehaviour(
        httpExchange: HttpExchange,
        pluginConfig: PluginConfig,
        resourceConfig: BasicResourceConfig?,
        additionalContext: Map<String, Any>?,
        statusCodeFactory: StatusCodeFactory,
        responseBehaviourFactory: ResponseBehaviourFactory,
    ): ResponseBehaviour {
        val responseConfig = resourceConfig?.responseConfig
        checkNotNull(responseConfig) { "Response configuration must not be null" }

        val statusCode = statusCodeFactory.calculateStatus(resourceConfig)
        val responseBehaviour: ReadWriteResponseBehaviour

        val steps = stepService.determineSteps(pluginConfig, resourceConfig)
        if (logger.isTraceEnabled) {
            logger.trace("{} processing steps for request: {}", steps.size, LogUtil.describeRequestShort(httpExchange))
        }
        if (steps.isEmpty()) {
            if (logger.isTraceEnabled) {
                logger.trace(
                    "Using default HTTP {} response behaviour for request: {}",
                    statusCode,
                    LogUtil.describeRequestShort(httpExchange),
                )
            }
            responseBehaviour = responseBehaviourFactory.build(statusCode, responseConfig)
        } else {
            val responseBehaviours = steps.map {
                it.step.execute(
                    it.context,
                    httpExchange,
                    statusCode,
                    responseBehaviourFactory,
                    additionalContext,
                )
            }
            // only the last response behaviour is used
            responseBehaviour = responseBehaviours.last()
        }

        // explicitly check if the root resource should have its response config used as defaults for its child resources
        when {
            pluginConfig is ResourcesHolder<*> && pluginConfig.isDefaultsFromRootResponse == true -> {
                if (pluginConfig is BasicResourceConfig) {
                    logger.trace("Inheriting root response configuration as defaults")
                    responseBehaviourFactory.populate(
                        statusCode,
                        (pluginConfig as BasicResourceConfig).responseConfig,
                        responseBehaviour
                    )
                }
            }
        }
        return responseBehaviour
    }
}
