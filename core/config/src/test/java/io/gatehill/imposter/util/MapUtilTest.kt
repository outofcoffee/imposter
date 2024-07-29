package io.gatehill.imposter.util

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Tests for [MapUtil].
 */
class MapUtilTest {
    @Test
    fun `jsonify should support Vertx JsonObject and JsonArray`() {
        val json = MapUtil.jsonify(
            JsonObject().put(
                "foo", JsonArray().add("bar")
            )
        )
        assertEquals("""
{
  "foo" : [ "bar" ]
}
""".trim(), json)
    }

    @Test
    fun `jsonify should support JSR-310 times`() {
        val json = MapUtil.jsonify(mapOf(
            "foo" to ZonedDateTime.of(2024, 7, 29, 0, 0, 0, 0, ZoneId.of("UTC"))
        ))
        assertEquals("""
{
  "foo" : "2024-07-29T00:00:00Z"
}
""".trim(), json)
    }
}
