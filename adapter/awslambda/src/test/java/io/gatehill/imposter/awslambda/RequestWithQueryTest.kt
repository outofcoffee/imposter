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

package io.gatehill.imposter.awslambda

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.tests.annotations.Event
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest

/**
 * Test event handling for requests with queries.
 */
class RequestWithQueryTest : AbstractHandlerTest() {
    private var handlerV1: Handler? = null
    private var handlerV2: HandlerV2? = null

    override val configDir = "/request-with-query/config"

    @BeforeEach
    fun setUp() {
        configure()
        handlerV1 = Handler()
        handlerV2 = HandlerV2()
    }

    @ParameterizedTest
    @Event(value = "request-with-query/requests_v1/request_with_query.json", type = APIGatewayProxyRequestEvent::class)
    fun `v1 request with query param`(event: APIGatewayProxyRequestEvent) {
        val responseEvent = handlerV1!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(200, responseEvent.statusCode)
        assertEquals("""{ "foo": "bar" }""", responseEvent.body)
    }

    @ParameterizedTest
    @Event(value = "request-with-query/requests_v2/request_with_query.json", type = APIGatewayV2HTTPEvent::class)
    fun `v2 request with query param`(event: APIGatewayV2HTTPEvent) {
        val responseEvent = handlerV2!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(200, responseEvent.statusCode)
        assertEquals("""{ "foo": "bar" }""", responseEvent.body)
    }
}
