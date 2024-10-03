package io.gatehill.imposter.http

import io.vertx.core.Vertx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

/**
 * Tests for [HttpRouter].
 */
class HttpRouterTest {
    @Test
    fun `route should add new route`() {
        val router = HttpRouter(Vertx.vertx())
        router.route("/test")
        assertEquals(1, router.routes.size)
        assertEquals("/test", router.routes[0].path)
    }

    @Test
    fun `route should replace existing route with same path and method`() {
        val router = HttpRouter(Vertx.vertx())
        router.route(HttpMethod.GET, "/test")
        router.route(HttpMethod.GET, "/test")
        assertEquals(1, router.routes.size)
    }

    @Test
    fun `get should add GET route`() {
        val router = HttpRouter(Vertx.vertx())
        val route = router.get("/test")
        assertEquals(HttpMethod.GET, route.method)
        assertEquals("/test", route.path)
    }

    @Test
    fun `post should add POST route`() {
        val router = HttpRouter(Vertx.vertx())
        val route = router.post("/test")
        assertEquals(HttpMethod.POST, route.method)
        assertEquals("/test", route.path)
    }

    @Test
    fun `errorHandler should add error handler`() {
        val router = HttpRouter(Vertx.vertx())
        val handler: HttpExchangeHandler = { }
        router.errorHandler(404, handler)
        assertEquals(handler, router.errorHandlers[404])
    }

    @Test
    fun `route with path params should be matched`() {
        val router = HttpRouter(Vertx.vertx())
        val route = router.route("/test/{param}")
        assertTrue(route.matches("/test/123"))

        // no params should be normalised
        assertEquals(router.normalisedParams.size, 0)
    }

    @Test
    fun `route with path params containing underscores should be matched`() {
        val router = HttpRouter(Vertx.vertx())
        val route = router.route("/test/{param_name}")
        assertTrue(route.matches("/test/123"))

        assertEquals(router.normalisedParams.size, 1)
        assertTrue(router.normalisedParams.values.contains("param_name"))
    }

    @Test
    fun `route with multiple path params should be matched`() {
        val router = HttpRouter(Vertx.vertx())
        val route = router.route("/test/{param_name}/then/{another_param}")
        assertTrue(route.matches("/test/123/then/456"))

        assertEquals(router.normalisedParams.size, 2)
        assertTrue(router.normalisedParams.values.contains("param_name"))
        assertTrue(router.normalisedParams.values.contains("another_param"))
    }

    @Test
    fun `invokeBeforeEndHandlers should call all before end handlers`() {
        val router = HttpRouter(Vertx.vertx())
        var called = false
        val handler: HttpExchangeHandler = { called = true }
        router.onBeforeEnd(handler)
        router.invokeBeforeEndHandlers(mock())
        assertTrue(called)
    }
}
