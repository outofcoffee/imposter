/*
 * Copyright (c) 2024.
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

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.script.ResponseBehaviourType
import io.gatehill.imposter.util.LogUtil
import io.gatehill.imposter.util.makeFuture
import io.gatehill.imposter.util.supervisedDefaultCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * Executes request interceptors, indicating whether the exchange
 * has been handled.
 */
class InterceptorServiceImpl @Inject constructor(
    private val responseRoutingService: ResponseRoutingService,
) : InterceptorService, CoroutineScope by supervisedDefaultCoroutineScope {

    override fun executeInterceptors(
        pluginConfig: PluginConfig,
        interceptors: List<BasicResourceConfig>,
        httpExchange: HttpExchange,
    ) = future {
        if (interceptors.isEmpty()) {
            return@future false
        }
        val handled = AtomicBoolean(true)
        val handler = buildHandler(httpExchange, handled)

        for (interceptor in interceptors) {
            responseRoutingService.route(
                pluginConfig,
                interceptor,
                httpExchange,
                handler,
            ).await()

            LOGGER.trace(
                "Interceptor {} handled for {}: {}",
                interceptor,
                LogUtil.describeRequestShort(httpExchange),
                handled
            )
            if (handled.get()) {
                break
            }
        }
        return@future handled.get()
    }

    private fun buildHandler(
        httpExchange: HttpExchange,
        handled: AtomicBoolean,
    ): DefaultBehaviourHandler = { responseBehaviour ->
        makeFuture {
            when (responseBehaviour.behaviourType) {
                ResponseBehaviourType.SHORT_CIRCUIT -> {
                    LOGGER.trace(
                        "Interceptor short-circuited {} with response behaviour {}",
                        LogUtil.describeRequestShort(httpExchange),
                        responseBehaviour,
                    )
                    handled.set(true)
                }

                ResponseBehaviourType.DEFAULT_BEHAVIOUR -> {
                    LOGGER.trace(
                        "Interceptor triggered continue to next for {}",
                        LogUtil.describeRequestShort(httpExchange),
                    )
                    handled.set(false)
                }

                else -> throw IllegalStateException("Response behaviour type must be set")
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(InterceptorServiceImpl::class.java)
    }
}
