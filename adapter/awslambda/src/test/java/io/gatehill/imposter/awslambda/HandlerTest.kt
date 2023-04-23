/*
 * Copyright (c) 2016-2023.
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

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.tests.annotations.Event
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.mockito.Mockito.mock

/**
 * Test event handling for V1 events.
 */
class HandlerTest : AbstractHandlerTest() {
    private var handler: Handler? = null
    private var context: Context? = null

    @BeforeEach
    fun setUp() {
        handler = Handler()
        context = mock(Context::class.java)
    }

    @ParameterizedTest
    @Event(value = "requests_v1/request_spec_example.json", type = APIGatewayProxyRequestEvent::class)
    fun `get example from spec`(event: APIGatewayProxyRequestEvent) {
        val responseEvent = handler!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(200, responseEvent.statusCode)
        assertEquals("""{ "id": 1, "name": "Cat" }""", responseEvent.body)
        assertEquals(4, responseEvent.headers?.size)
        assertEquals("imposter", responseEvent.headers["Server"])
    }

    @ParameterizedTest
    @Event(value = "requests_v1/request_file.json", type = APIGatewayProxyRequestEvent::class)
    fun `get static file`(event: APIGatewayProxyRequestEvent) {
        val responseEvent = handler!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(200, responseEvent.statusCode)
        assertEquals("""{ "id": 2, "name": "Dog" }""", responseEvent.body)
        assertEquals(4, responseEvent.headers?.size)
        assertEquals("imposter", responseEvent.headers["Server"])
    }

    @ParameterizedTest
    @Event(value = "requests_v1/request_no_route.json", type = APIGatewayProxyRequestEvent::class)
    fun `no matching route`(event: APIGatewayProxyRequestEvent) {
        val responseEvent = handler!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(404, responseEvent.statusCode)
        assertEquals("text/plain", responseEvent.headers["Content-Type"])
        assertEquals(2, responseEvent.headers?.size)
        assertEquals("Resource not found", responseEvent.body)
    }

    @ParameterizedTest
    @Event(value = "requests_v1/request_404_html.json", type = APIGatewayProxyRequestEvent::class)
    fun `get HTML response for 404`(event: APIGatewayProxyRequestEvent) {
        val responseEvent = handler!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(404, responseEvent.statusCode)
        assertEquals("text/html", responseEvent.headers["Content-Type"])
    }

    @ParameterizedTest
    @Event(value = "requests_v1/request_static_asset.json", type = APIGatewayProxyRequestEvent::class)
    fun `should load static files`(event: APIGatewayProxyRequestEvent) {
        val responseEvent = handler!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(200, responseEvent.statusCode)
        assertThat(responseEvent.body, containsString(".example"))
    }

    @ParameterizedTest
    @Event(value = "requests_v1/request_status.json", type = APIGatewayProxyRequestEvent::class)
    fun `should fetch version from status endpoint`(event: APIGatewayProxyRequestEvent) {
        val responseEvent = handler!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(200, responseEvent.statusCode)
        assertThat(responseEvent.body, not(containsString("unknown")))
    }
}
