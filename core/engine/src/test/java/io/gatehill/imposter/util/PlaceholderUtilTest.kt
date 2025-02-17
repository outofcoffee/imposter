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

package io.gatehill.imposter.util

import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.vertx.core.buffer.Buffer
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.number.OrderingComparison
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

class PlaceholderUtilTest {
    @Test
    fun `eval request header`() {
        val request = mock<HttpRequest> {
            on { getHeader(eq("Correlation-ID")) } doReturn "foo"
        }
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn request
        }

        val result = PlaceholderUtil.replace(
            input = "\${context.request.headers.Correlation-ID}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        assertThat(result, equalTo("foo"))
    }

    @Test
    fun `eval request path param`() {
        val httpRequest = mock<HttpRequest> {
            on { getPathParam("userId") } doReturn "Example-User-ID"
        }
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn httpRequest
        }

        val result = PlaceholderUtil.replace(
            input = "\${context.request.pathParams.userId}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        assertThat(result, equalTo("Example-User-ID"))
    }

    @Test
    fun `eval request query param`() {
        val httpRequest = mock<HttpRequest> {
            on { getQueryParam("page") } doReturn "1"
        }
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn httpRequest
        }

        val result = PlaceholderUtil.replace(
            input = "\${context.request.queryParams.page}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        assertThat(result, equalTo("1"))
    }

    @Test
    fun `eval request body`() {
        val httpRequest = mock<HttpRequest> {
            on { bodyAsString } doReturn "Request body"
        }
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn httpRequest
        }

        val result = PlaceholderUtil.replace(
            input = "\${context.request.body}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        assertThat(result, equalTo("Request body"))
    }

    @Test
    fun `eval request body with JsonPath`() {
        val httpRequest = mock<HttpRequest> {
            on { bodyAsString } doReturn """{ "name": "Ada" }"""
        }
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn httpRequest
        }

        val result = PlaceholderUtil.replace(
            input = "\${context.request.body:$.name}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        assertThat(result, equalTo("Ada"))
    }

    @Test
    fun `eval missing request path param with fallback`() {
        val httpRequest = mock<HttpRequest> {
            on { getPathParam(eq("foo")) } doReturn null
        }
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn httpRequest
        }

        val result = PlaceholderUtil.replace(
            input = "\${context.request.pathParams.foo:-fallback}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        assertThat(result, equalTo("fallback"))
    }

    @Test
    fun `eval response body`() {
        val response = mock<HttpResponse> {
            on { bodyBuffer } doReturn Buffer.buffer("Response body")
        }
        val httpExchange = mock<HttpExchange> {
            on { phase } doReturn ExchangePhase.RESPONSE_SENT
            on { this.response } doReturn response
        }

        val result = PlaceholderUtil.replace(
            input = "\${context.response.body}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        assertThat(result, equalTo("Response body"))
    }

    @Test
    fun `eval response header`() {
        val response = mock<HttpResponse> {
            on { getHeader(eq("X-Example")) } doReturn "foo"
        }
        val httpExchange = mock<HttpExchange> {
            on { phase } doReturn ExchangePhase.RESPONSE_SENT
            on { this.response } doReturn response
        }

        val result = PlaceholderUtil.replace(
            input = "\${context.response.headers.X-Example}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        assertThat(result, equalTo("foo"))
    }

    @Test
    fun `fail to eval response in wrong phase`() {
        val httpExchange = mock<HttpExchange> {
            on { phase } doReturn ExchangePhase.REQUEST_RECEIVED
        }

        checkExceptionThrownForPhase(httpExchange, "\${context.response.body}")
        checkExceptionThrownForPhase(httpExchange, "\${context.response.headers.foo}")
    }

    private fun checkExceptionThrownForPhase(httpExchange: HttpExchange, expression: String) {
        var cause: Throwable? = null
        try {
            PlaceholderUtil.replace(
                input = expression,
                httpExchange = httpExchange,
                evaluators = PlaceholderUtil.defaultEvaluators,
            )
            fail("IllegalStateException should have been thrown")

        } catch (e: Exception) {
            cause = e.cause
            while (cause?.cause != null) {
                cause = cause.cause
            }
        }
        assertEquals(IllegalStateException::class.java, cause?.javaClass, "IllegalStateException should be root cause")
    }

    @Test
    fun `eval with multiple expressions`() {
        val request = mock<HttpRequest> {
            on { getHeader(eq("Correlation-ID")) } doReturn "foo"
            on { getHeader(eq("User-Agent")) } doReturn "mozilla"
        }
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn request
        }

        val result = PlaceholderUtil.replace(
            input = "\${context.request.headers.Correlation-ID}_\${context.request.headers.User-Agent}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )

        assertThat(result, equalTo("foo_mozilla"))
    }

    @Test
    fun `eval datetime`() {
        val httpExchange = mock<HttpExchange>()

        val millis = PlaceholderUtil.replace(
            input = "\${datetime.now.millis}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )
        assertThat(millis.toLong(), OrderingComparison.lessThanOrEqualTo(System.currentTimeMillis()))

        val nanos = PlaceholderUtil.replace(
            input = "\${datetime.now.nanos}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )
        assertThat(nanos.toLong(), OrderingComparison.lessThanOrEqualTo(System.nanoTime()))

        val iso8601Date = PlaceholderUtil.replace(
            input = "\${datetime.now.iso8601_date}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )
        try {
            DateTimeUtil.DATE_FORMATTER.parse(iso8601Date)
        } catch (e: Exception) {
            fail("Failed to parse ISO-8601 date: ${e.message}")
        }

        val iso8601DateTime = PlaceholderUtil.replace(
            input = "\${datetime.now.iso8601_datetime}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )
        try {
            DateTimeUtil.DATE_TIME_FORMATTER.parse(iso8601DateTime)
        } catch (e: Exception) {
            fail("Failed to parse ISO-8601 dateTime: ${e.message}")
        }
    }

    @Test
    fun `eval invalid expression`() {
        val httpExchange = mock<HttpExchange>()

        val result = PlaceholderUtil.replace(
            input = "\${invalid}",
            httpExchange = httpExchange,
            evaluators = PlaceholderUtil.defaultEvaluators,
        )
        assertThat(result, equalTo(""))
    }
}
