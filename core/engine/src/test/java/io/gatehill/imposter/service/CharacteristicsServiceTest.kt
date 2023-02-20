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

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.script.FailureSimulationType
import io.gatehill.imposter.script.PerformanceSimulationConfig
import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Tests for [CharacteristicsService].
 */
class CharacteristicsServiceTest {
    @Test
    fun `should simulate performance for exact delay`() {
        val responseBehaviour = buildPerformanceSim(
            PerformanceSimulationConfig(
                exactDelayMs = 1000,
            )
        )
        val service = CharacteristicsService(mock())
        val delayMs = service.simulatePerformance(responseBehaviour)
        assertEquals(1000, delayMs)
    }

    @Test
    fun `should simulate performance for range`() {
        val responseBehaviour = buildPerformanceSim(
            PerformanceSimulationConfig(
                minDelayMs = 1000,
                maxDelayMs = 2000,
            )
        )

        val service = CharacteristicsService(mock())
        val delayMs = service.simulatePerformance(responseBehaviour)
        assertTrue(delayMs in 1000..2000)
    }

    private fun buildPerformanceSim(perfConfig: PerformanceSimulationConfig) = ReadWriteResponseBehaviourImpl().apply {
        performanceSimulation = perfConfig
    }

    @Test
    fun `should send failure for close connection`() {
        val httpResponse = sendFailureType(FailureSimulationType.CloseConnection)
        verify(httpResponse).close()
    }

    @Test
    fun `should send failure for empty response`() {
        val httpResponse = sendFailureType(FailureSimulationType.EmptyResponse)
        verify(httpResponse).end()
    }

    private fun sendFailureType(failureType: FailureSimulationType): HttpResponse {
        val responseService = mock<ResponseService> {
            on { finaliseExchange(any(), any(), any()) } doAnswer {
                val block = it.arguments[2] as () -> Unit
                block()
            }
        }
        val httpRequest = mock<HttpRequest> {
            on { method } doReturn HttpMethod.GET
            on { path } doReturn "/foo"
        }
        val httpResponse = mock<HttpResponse>()
        val httpExchange = mock<HttpExchange> {
            on { request } doReturn httpRequest
            on { response } doReturn httpResponse
        }

        val service = CharacteristicsService(responseService)
        service.sendFailure(mock(), httpExchange, failureType)

        verify(responseService).finaliseExchange(any(), eq(httpExchange), any())
        return httpResponse
    }
}
