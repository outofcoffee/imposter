/*
 * Copyright (c) 2016-2023.
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
package io.gatehill.imposter.plugin.openapi

import io.gatehill.imposter.server.BaseVerticleTest
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxTestContext
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.function.Consumer

/**
 * Tests for [OpenApiPluginImpl].
 *
 * @author Pete Cornish
 */
class OpenApiPluginImplTest : BaseVerticleTest() {
    override val pluginClass = OpenApiPluginImpl::class.java

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/openapi2/simple",
        "/openapi3/simple"
    )

    private fun assertBody(body: String) {
        assertNotNull(body)
        val jsonBody = JsonObject(body)
        val versions = jsonBody.getJsonArray("versions")
        assertNotNull(versions, "Versions array should exist")
        assertEquals(2, versions.size())

        // verify entries
        assertNotNull(versions.getJsonObject(0), "Version array entry 0 should exist")
        assertNotNull(versions.getJsonObject(1), "Version array entry 1 should exist")
    }

    /**
     * Should return the example from the specification for the default HTTP 200 status code, since the
     * content type in the 'Accept' header matches that in the specification example.
     */
    @Test
    fun testServeDefaultExampleMatchContentType() {
        val body = RestAssured.given()
            .log().ifValidationFails() // JSON content type in 'Accept' header matches specification example
            .accept(ContentType.JSON)
            .`when`().get("/simple/apis")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .extract().asString()

        assertBody(body)
    }

    /**
     * Should return the example from the specification for the default HTTP 200 status code, even though the
     * content type in the 'Accept' header does not match that in the specification example.
     *
     * @see OpenApiPluginConfig.isPickFirstIfNoneMatch
     */
    @Test
    fun testServeDefaultExampleNoExactMatch() {
        val body = RestAssured.given()
            .log().ifValidationFails() // do not set JSON content type in 'Accept' header, to force mismatch against specification example
            .accept(ContentType.TEXT)
            .`when`().get("/simple/apis")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .extract().asString()

        assertBody(body)
    }

    /**
     * Should return the specification UI.
     */
    @Test
    fun testGetSpecUi() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.TEXT)
            .`when`().get("/_spec/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .extract().asString()

        assertTrue(body.contains("</html>"))
    }

    /**
     * Should return a combined specification.
     */
    @Test
    fun testGetCombinedSpec() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.JSON)
            .`when`().get("/_spec/combined.json")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .extract().asString()

        assertNotNull(body)
        val parseResult = OpenAPIV3Parser().readContents(body, emptyList(), ParseOptions())
        assertNotNull(parseResult)

        val combined = parseResult.openAPI
        assertNotNull(combined)
        assertNotNull(combined.info)
        assertEquals("Imposter Mock APIs", combined.info.title)

        // should contain combination of all specs' endpoints
        assertEquals(6, combined.paths.size)

        // should contain mock server endpoint
        assertTrue(combined.servers.any { it.url == "http://$host:$listenPort/simple" })

        // OASv2
        assertTrue(combined.paths.containsKey("/apis"))
        assertTrue(combined.paths.containsKey("/v2"))
        assertTrue(combined.paths.containsKey("/pets"))
        assertTrue(combined.paths.containsKey("/pets/{id}"))

        // OASv3
        assertTrue(combined.paths.containsKey("/oas3/apis"))
        assertTrue(combined.paths.containsKey("/oas3/v2"))
    }

    /**
     * Should return examples formatted as JSON.
     */
    @Test
    fun testExamples() {
        // OASv2
        queryEndpoint("/simple/apis") { responseBody: String ->
            val trimmed = responseBody.trim { it <= ' ' }
            assertTrue(trimmed.startsWith("{"))
            assertTrue(trimmed.contains("CURRENT"))
            assertTrue(trimmed.endsWith("}"))
        }
        queryEndpoint("/api/pets/1") { responseBody: String ->
            val trimmed = responseBody.trim { it <= ' ' }
            assertTrue(trimmed.startsWith("{"))
            assertTrue(trimmed.contains("Fluffy"))
            assertTrue(trimmed.endsWith("}"))
        }

        // OASv3
        queryEndpoint("/oas3/apis") { responseBody: String ->
            val trimmed = responseBody.trim { it <= ' ' }
            assertTrue(trimmed.startsWith("{"))
            assertTrue(trimmed.contains("CURRENT"))
            assertTrue(trimmed.endsWith("}"))
        }
    }

    private fun queryEndpoint(url: String, bodyConsumer: Consumer<String>) {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.JSON)
            .`when`().get(url)
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .extract().asString()

        bodyConsumer.accept(body)
    }
}