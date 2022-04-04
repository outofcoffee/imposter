package io.gatehill.imposter.store.service

import com.jayway.jsonpath.internal.path.PathCompiler.fail
import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.util.DateTimeUtil
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.impl.headers.HeadersMultiMap
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.number.OrderingComparison
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

class ExpressionServiceImplTest {
    private lateinit var service: ExpressionServiceImpl

    @Before
    fun setUp() {
        service = ExpressionServiceImpl()
    }

    @Test
    fun `eval request header`() {
        val request = mock<HttpRequest> {
            on { getHeader(eq("Correlation-ID")) } doReturn "foo"
        }
        val httpExchange = mock<HttpExchange> {
            on { request() } doReturn request
        }

        val result = service.eval(
            expression = "\${context.request.headers.Correlation-ID}",
            httpExchange = httpExchange,
        )

        assertThat(result, equalTo("foo"))
    }

    @Test
    fun `eval request path param`() {
        val httpExchange = mock<HttpExchange> {
            on { pathParam("userId") } doReturn "Example-User-ID"
        }

        val result = service.eval(
            expression = "\${context.request.pathParams.userId}",
            httpExchange = httpExchange,
        )

        assertThat(result, equalTo("Example-User-ID"))
    }

    @Test
    fun `eval request query param`() {
        val httpExchange = mock<HttpExchange> {
            on { queryParam("page") } doReturn "1"
        }

        val result = service.eval(
            expression = "\${context.request.queryParams.page}",
            httpExchange = httpExchange,
        )

        assertThat(result, equalTo("1"))
    }

    @Test
    fun `eval request body`() {
        val httpExchange = mock<HttpExchange> {
            on { bodyAsString } doReturn "Request body"
        }

        val result = service.eval(
            expression = "\${context.request.body}",
            httpExchange = httpExchange,
        )

        assertThat(result, equalTo("Request body"))
    }

    @Test
    fun `eval request body with JsonPath`() {
        val httpExchange = mock<HttpExchange> {
            on { bodyAsString } doReturn """{ "name": "Ada" }"""
        }

        val result = service.eval(
            expression = "\${context.request.body:$.name}",
            httpExchange = httpExchange,
        )

        assertThat(result, equalTo("Ada"))
    }

    @Test
    fun `eval response body`() {
        val response = mock<HttpResponse> {
            on { bodyBuffer } doReturn Buffer.buffer("Response body")
        }
        val httpExchange = mock<HttpExchange> {
            on { phase } doReturn ExchangePhase.RESPONSE_SENT
            on { response() } doReturn response
        }

        val result = service.eval(
            expression = "\${context.response.body}",
            httpExchange = httpExchange,
        )

        assertThat(result, equalTo("Response body"))
    }

    @Test
    fun `eval response header`() {
        val response = mock<HttpResponse> {
            on { headers() } doReturn HeadersMultiMap.headers().apply {
                add("X-Example", "foo")
            }
        }
        val httpExchange = mock<HttpExchange> {
            on { phase } doReturn ExchangePhase.RESPONSE_SENT
            on { response() } doReturn response
        }

        val result = service.eval(
            expression = "\${context.response.headers.X-Example}",
            httpExchange = httpExchange,
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
            service.eval(
                expression = expression,
                httpExchange = httpExchange,
            )
            fail("IllegalStateException should have been thrown")

        } catch (e: Exception) {
            cause = e.cause
            while (cause?.cause != null) {
                cause = cause.cause
            }
        }
        assertEquals("IllegalStateException should be root cause", IllegalStateException::class.java, cause?.javaClass)
    }

    @Test
    fun `eval with multiple expressions`() {
        val request = mock<HttpRequest> {
            on { getHeader(eq("Correlation-ID")) } doReturn "foo"
            on { getHeader(eq("User-Agent")) } doReturn "mozilla"
        }
        val httpExchange = mock<HttpExchange> {
            on { request() } doReturn request
        }

        val result = service.eval(
            expression = "\${context.request.headers.Correlation-ID}_\${context.request.headers.User-Agent}",
            httpExchange = httpExchange,
        )

        assertThat(result, equalTo("foo_mozilla"))
    }

    @Test
    fun `eval datetime`() {
        val httpExchange = mock<HttpExchange>()

        val millis = service.eval(
            expression = "\${datetime.now.millis}",
            httpExchange = httpExchange,
        )
        assertThat(millis.toLong(), OrderingComparison.lessThanOrEqualTo(System.currentTimeMillis()))

        val nanos = service.eval(
            expression = "\${datetime.now.nanos}",
            httpExchange = httpExchange,
        )
        assertThat(nanos.toLong(), OrderingComparison.lessThanOrEqualTo(System.nanoTime()))

        val iso8601Date = service.eval(
            expression = "\${datetime.now.iso8601_date}",
            httpExchange = httpExchange,
        )
        try {
            DateTimeUtil.DATE_FORMATTER.parse(iso8601Date)
        } catch (e: Exception) {
            fail("Failed to parse ISO-8601 date: ${e.message}")
        }

        val iso8601DateTime = service.eval(
            expression = "\${datetime.now.iso8601_datetime}",
            httpExchange = httpExchange,
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

        val result = service.eval(
            expression = "\${invalid}",
            httpExchange = httpExchange,
        )
        assertThat(result, equalTo(""))
    }
}
