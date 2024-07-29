package io.gatehill.imposter.util

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

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
}
