package io.gatehill.imposter.store.service

import com.jayway.jsonpath.internal.path.PathCompiler.fail
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.util.DateTimeUtil
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.number.OrderingComparison
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

class ExpressionServiceImplTest {
    private lateinit var service: ExpressionServiceImpl;

    @Before
    fun setUp() {
        service = ExpressionServiceImpl()
    }

    @Test
    fun `eval header with expression`() {
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

        assertThat(result, CoreMatchers.equalTo("foo"))
    }

    @Test
    fun `eval path param with expression`() {
        val httpExchange = mock<HttpExchange> {
            on { pathParam("userId") } doReturn "Example-User-ID"
        }

        val result = service.eval(
            expression = "\${context.request.pathParams.userId}",
            httpExchange = httpExchange,
        )

        assertThat(result, CoreMatchers.equalTo("Example-User-ID"))
    }

    @Test
    fun `eval query param with expression`() {
        val httpExchange = mock<HttpExchange> {
            on { queryParam("page") } doReturn "1"
        }

        val result = service.eval(
            expression = "\${context.request.queryParams.page}",
            httpExchange = httpExchange,
        )

        assertThat(result, CoreMatchers.equalTo("1"))
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

        assertThat(result, CoreMatchers.equalTo("foo_mozilla"))
    }

    @Test
    fun `eval datetime`() {
        val httpExchange = mock<HttpExchange>()

        val millis = service.eval(
            expression = "\${datetime.now.millis}",
            httpExchange = httpExchange,
        )
        assertThat(millis?.toLong(), OrderingComparison.lessThanOrEqualTo(System.currentTimeMillis()))

        val nanos = service.eval(
            expression = "\${datetime.now.nanos}",
            httpExchange = httpExchange,
        )
        assertThat(nanos?.toLong(), OrderingComparison.lessThanOrEqualTo(System.nanoTime()))

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
    fun `eval invalid`() {
        val httpExchange = mock<HttpExchange>()

        val result = service.eval(
            expression = "\${invalid}",
            httpExchange = httpExchange,
        )
        assertThat(result, equalTo(""))
    }
}
