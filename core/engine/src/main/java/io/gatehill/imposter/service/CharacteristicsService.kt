/*
 * Copyright (c) 2023-2023.
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
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.script.FailureSimulationType
import io.gatehill.imposter.script.ResponseBehaviour
import io.gatehill.imposter.util.LogUtil
import org.apache.logging.log4j.LogManager
import javax.inject.Inject
import kotlin.random.Random

/**
 * Provides response characteristics, such as performance and failure simulation.
 */
class CharacteristicsService @Inject constructor(
    private val responseService: ResponseService,
) {
    fun simulatePerformance(responseBehaviour: ResponseBehaviour): Int {
        val performance = responseBehaviour.performanceSimulation
        var delayMs = -1
        performance?.let {
            performance.exactDelayMs?.takeIf { it > 0 }?.let { exactDelayMs ->
                delayMs = exactDelayMs
            } ?: run {
                val minDelayMs = performance.minDelayMs ?: 0
                val maxDelayMs = performance.maxDelayMs ?: 0
                if (minDelayMs > 0 && maxDelayMs >= minDelayMs) {
                    delayMs = Random.nextInt(minDelayMs, maxDelayMs)
                }
            }
        }
        return delayMs
    }

    fun sendFailure(
        resourceConfig: ResourceConfig?,
        httpExchange: HttpExchange,
        failureType: FailureSimulationType,
    ) {
        LOGGER.trace(
            "Simulating {} failure for {}",
            failureType,
            LogUtil.describeRequestShort(httpExchange),
        )
        responseService.finaliseExchange(resourceConfig, httpExchange) {
            try {
                when (failureType) {
                    FailureSimulationType.EmptyResponse -> httpExchange.response().end()
                    FailureSimulationType.CloseConnection -> httpExchange.response().close()
                }
            } catch (e: Exception) {
                httpExchange.fail(
                    ResponseException("Error simulating $failureType failure for " + LogUtil.describeRequest(httpExchange), e)
                )
            }
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(CharacteristicsService::class.java)
    }
}
