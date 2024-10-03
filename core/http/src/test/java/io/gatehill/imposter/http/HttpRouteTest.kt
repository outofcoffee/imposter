package io.gatehill.imposter.http

import io.vertx.core.Vertx
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for [HttpRoute].
 */
class HttpRouteTest {
    @Test
    fun `should match exact path`() {
        val router = HttpRouter(Vertx.vertx())
        val route = HttpRoute(router, path = "/foo")
        val matches = route.matches("/foo")
        assertTrue(matches)
    }

    @Test
    fun `should match path with trailing wildcard`() {
        val router = HttpRouter(Vertx.vertx())
        val route = HttpRoute(router, path = "/foo*")
        val matches = route.matches("/foo/bar")
        assertTrue(matches)
    }

    @Test
    fun `should match path with placeholder`() {
        val router = HttpRouter(Vertx.vertx())
        val route = HttpRoute(router, path = "/foo/{id}")
        val matches = route.matches("/foo/123")
        assertTrue(matches)
    }
}
