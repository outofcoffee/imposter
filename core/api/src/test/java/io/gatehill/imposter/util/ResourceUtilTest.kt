package io.gatehill.imposter.util

import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.plugin.config.resource.ResourceConfig
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Tests for [ResourceUtil].
 */
class ResourceUtilTest {
    @Test
    fun `convertPathParamsToBracketFormat should convert colon format to bracket format`() {
        val result = ResourceUtil.convertPathParamsToBracketFormat("/example/:foo")
        assertEquals("/example/{foo}", result)
    }

    @Test
    fun `convertPathParamsToBracketFormat should support mix of placeholders and strings`() {
        assertEquals(
            "/example/{foo}-bar",
            ResourceUtil.convertPathParamsToBracketFormat("/example/:foo-bar"),
        )
        assertEquals(
            "/example/{foo}.bar",
            ResourceUtil.convertPathParamsToBracketFormat("/example/:foo.bar"),
        )
        assertEquals(
            "/example/baz.{foo}",
            ResourceUtil.convertPathParamsToBracketFormat("/example/baz.:foo"),
        )
    }

    @Test
    fun `convertPathParamsToBracketFormat should return null when input is null`() {
        val result = ResourceUtil.convertPathParamsToBracketFormat(null)
        assertNull(result)
    }

    @Test
    fun `extractResourceMethod should return method from MethodResourceConfig`() {
        val resourceConfig = RestResourceConfig().apply { method = HttpMethod.POST }
        val result = ResourceUtil.extractResourceMethod(resourceConfig)
        assertEquals(HttpMethod.POST, result)
    }

    @Test
    fun `extractResourceMethod should return GET when resourceConfig is not MethodResourceConfig`() {
        val resourceConfig = object : ResourceConfig {
            override var path: String? = "/example"
        }
        val result = ResourceUtil.extractResourceMethod(resourceConfig)
        assertEquals(HttpMethod.GET, result)
    }
}
