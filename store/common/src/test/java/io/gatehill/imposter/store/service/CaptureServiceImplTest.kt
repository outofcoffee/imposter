package io.gatehill.imposter.store.service

import com.jayway.jsonpath.DocumentContext
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.lifecycle.EngineLifecycleHooks
import io.gatehill.imposter.plugin.config.capture.CaptureConfig
import io.gatehill.imposter.plugin.config.capture.ItemCaptureConfig
import io.gatehill.imposter.store.model.Store
import io.gatehill.imposter.store.model.StoreFactory
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.concurrent.atomic.AtomicReference

class CaptureServiceImplTest {
    private lateinit var store: Store
    private lateinit var service: CaptureServiceImpl;

    @Before
    fun setUp() {
        store = mock()
        val storeFactory = mock<StoreFactory> {
            on { getStoreByName(any(), any()) } doReturn store
        }
        service = CaptureServiceImpl(
            storeFactory = storeFactory,
            lifecycleHooks = EngineLifecycleHooks(),
            expressionService = ExpressionServiceImpl(),
        )
    }

    @Test
    fun `capture header with expression`() {
        val request = mock<HttpRequest> {
            on { getHeader(eq("Correlation-ID")) } doReturn "foo"
        }
        val httpExchange = mock<HttpExchange> {
            on { request() } doReturn request
        }

        service.captureItem(
            captureConfigKey = "correlationId",
            itemConfig = ItemCaptureConfig(
                key = CaptureConfig(
                    expression = "\${context.request.headers.Correlation-ID}"
                ),
                constValue = "bar",
                store = "test",
            ),
            httpExchange = httpExchange,
            jsonPathContextHolder = AtomicReference<DocumentContext>(),
        )

        verify(store).save(eq("foo"), eq("bar"))
    }
}
