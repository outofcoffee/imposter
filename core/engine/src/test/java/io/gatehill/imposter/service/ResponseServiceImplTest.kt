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

import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig
import io.gatehill.imposter.script.ReadWriteResponseBehaviourImpl
import io.gatehill.imposter.util.HttpUtil
import io.vertx.core.buffer.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.io.File

/**
 * Tests for [ResponseServiceImpl].
 */
class ResponseServiceImplTest {
    @Test
    fun `should send empty response`() {
        val responseService = ResponseServiceImpl(mock(), mock(), mock(), mock())
        val httpRequest = mock<HttpRequest> {
            on { method } doReturn HttpMethod.GET
            on { path } doReturn "/foo"
        }
        val httpResponse = mock<HttpResponse> {
            on { setStatusCode(any()) } doReturn mock
        }
        val httpExchange = mock<HttpExchange> {
            on { request } doReturn httpRequest
            on { response } doReturn httpResponse
        }
        val responseBehaviour = ReadWriteResponseBehaviourImpl()
        responseService.sendEmptyResponse(httpExchange, responseBehaviour)

        verify(httpResponse).end()
    }

    @Test
    fun `should send content response`() {
        val responseService = ResponseServiceImpl(mock(), mock(), mock(), mock())
        val httpRequest = mock<HttpRequest> {
            on { method } doReturn HttpMethod.GET
            on { path } doReturn "/foo"
        }
        val httpResponse = mock<HttpResponse> {
            on { setStatusCode(any()) } doReturn mock
            on { statusCode } doReturn 200
            on { getHeader(eq(HttpUtil.CONTENT_TYPE)) } doReturn HttpUtil.CONTENT_TYPE_PLAIN_TEXT
            on { putHeader(any(), any()) } doReturn mock
            on { end(any<Buffer>()) } doAnswer {
                assertEquals("foo", (it.arguments[0] as Buffer).toString())
            }
        }
        val httpExchange = mock<HttpExchange> {
            on { request } doReturn httpRequest
            on { response } doReturn httpResponse
        }
        val responseBehaviour = ReadWriteResponseBehaviourImpl().apply {
            statusCode = 200
            content = "foo"
            responseHeaders[HttpUtil.CONTENT_TYPE] = HttpUtil.CONTENT_TYPE_PLAIN_TEXT
        }
        responseService.sendResponse(PluginConfigImpl(), RestResourceConfig(), httpExchange, responseBehaviour)

        verify(httpResponse).setStatusCode(eq(200))
        verify(httpResponse).getHeader(eq(HttpUtil.CONTENT_TYPE))
        verify(httpResponse).putHeader(eq(HttpUtil.CONTENT_TYPE), eq(HttpUtil.CONTENT_TYPE_PLAIN_TEXT))
        verify(httpResponse).end(any<Buffer>())
        verify(httpExchange).phase = ExchangePhase.RESPONSE_SENT
    }

    @Test
    fun `should send file response`() {
        val responseFileService = mock<ResponseFileService>()
        val responseService = ResponseServiceImpl(mock(), mock(), responseFileService, mock())
        val httpRequest = mock<HttpRequest> {
            on { method } doReturn HttpMethod.GET
            on { path } doReturn "/foo"
        }
        val httpResponse = mock<HttpResponse> {
            on { setStatusCode(any()) } doReturn mock
            on { statusCode } doReturn 200
            on { putHeader(any(), any()) } doReturn mock
            on { end(any<Buffer>()) } doAnswer {
                assertEquals("foo", (it.arguments[0] as Buffer).toString())
            }
        }
        val httpExchange = mock<HttpExchange> {
            on { request } doReturn httpRequest
            on { response } doReturn httpResponse
        }
        val responseBehaviour = ReadWriteResponseBehaviourImpl().apply {
            statusCode = 200
            responseFile = "response-file.txt"
            responseHeaders[HttpUtil.CONTENT_TYPE] = HttpUtil.CONTENT_TYPE_PLAIN_TEXT
        }
        val pluginConfig = PluginConfigImpl().apply {
            parentDir = File(ResponseFileServiceImplTest::class.java.getResource("/response-file.txt")!!.toURI()).parentFile
        }
        responseService.sendResponse(pluginConfig, RestResourceConfig(), httpExchange, responseBehaviour)

        verify(httpResponse).setStatusCode(eq(200))
        verify(httpResponse).putHeader(eq(HttpUtil.CONTENT_TYPE), eq(HttpUtil.CONTENT_TYPE_PLAIN_TEXT))
        verify(responseFileService).serveResponseFile(any(), any(), any(), any())
        verify(httpExchange).phase = ExchangePhase.RESPONSE_SENT
    }

    @Test
    fun `should send not found response`() {
        val responseService = ResponseServiceImpl(mock(), mock(), mock(), mock())
        val httpRequest = mock<HttpRequest> {
            on { method } doReturn HttpMethod.GET
            on { path } doReturn "/foo"
        }
        val httpResponse = mock<HttpResponse> {
            on { setStatusCode(any()) } doReturn mock
            on { statusCode } doReturn 404
            on { putHeader(any(), any()) } doReturn mock
        }
        val httpExchange = mock<HttpExchange> {
            on { request } doReturn httpRequest
            on { response } doReturn httpResponse
        }
        responseService.sendNotFoundResponse(httpExchange)

        verify(httpResponse).setStatusCode(eq(404))
        verify(httpResponse).putHeader(eq(HttpUtil.CONTENT_TYPE), eq(HttpUtil.CONTENT_TYPE_PLAIN_TEXT))
        verify(httpResponse).end(eq("Resource not found"))
        verify(httpExchange).phase = ExchangePhase.RESPONSE_SENT
    }

    @Test
    fun `should write response data`() {
        val responseService = ResponseServiceImpl(mock(), mock(), mock(), mock())
        val httpRequest = mock<HttpRequest> {
            on { method } doReturn HttpMethod.GET
            on { path } doReturn "/foo"
        }
        val httpResponse = mock<HttpResponse> {
            on { getHeader(eq(HttpUtil.CONTENT_TYPE)) } doReturn HttpUtil.CONTENT_TYPE_PLAIN_TEXT
            on { end(any<Buffer>()) } doAnswer {
                assertEquals("foo", (it.arguments[0] as Buffer).toString())
            }
        }
        val httpExchange = mock<HttpExchange> {
            on { request } doReturn httpRequest
            on { response } doReturn httpResponse
        }
        responseService.writeResponseData(RestResourceConfig(), httpExchange, null, Buffer.buffer("foo"), template = false, trustedData = false)

        verify(httpResponse).getHeader(eq(HttpUtil.CONTENT_TYPE))
        verify(httpResponse).end(any<Buffer>())
    }

    @Test
    fun `should finalise exchange`() {
        val responseService = ResponseServiceImpl(mock(), mock(), mock(), mock())
        val httpRequest = mock<HttpRequest> {
            on { method } doReturn HttpMethod.GET
            on { path } doReturn "/foo"
        }
        val httpResponse = mock<HttpResponse> {
            on { end(any<Buffer>()) } doAnswer {
                assertEquals("foo", (it.arguments[0] as Buffer).toString())
            }
        }
        val httpExchange = mock<HttpExchange> {
            on { request } doReturn httpRequest
            on { response } doReturn httpResponse
        }
        var blockExecuted = false
        responseService.finaliseExchange(RestResourceConfig(), httpExchange) {
            blockExecuted = true
        }

        assertTrue("the block should be executed", blockExecuted)
        verify(httpExchange).phase = ExchangePhase.RESPONSE_SENT
    }
}
