/*
 * Copyright (c) 2016-2021.
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

import com.jayway.restassured.RestAssured
import com.jayway.restassured.http.ContentType
import io.gatehill.imposter.server.BaseVerticleTest
import io.vertx.ext.unit.TestContext
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test

/**
 * Tests for request validation for OpenAPI mocks.
 *
 * @author Pete Cornish
 */
class RequestValidationTest : BaseVerticleTest() {
    override val pluginClass = OpenApiPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/openapi3/request-validation"
    )

    /**
     * Request should pass request validation.
     */
    @Test
    fun testValidRequest(testContext: TestContext?) {
        RestAssured.given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .`when`()
            .header("X-CorrelationID", "foo")
            .body("{ \"id\": 1, \"name\": \"Cat\" }")
            .post("/pets")
            .then()
            .log().ifValidationFails()
            .statusCode(201)
    }

    /**
     * Request should fail request validation due to nonconformant request body.
     */
    @Test
    fun testMissingRequestBody(testContext: TestContext?) {
        RestAssured.given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .`when`()
            .header("X-CorrelationID", "foo")
            .post("/pets")
            .then()
            .log().ifValidationFails()
            .statusCode(400)
            .body(Matchers.containsString("A request body is required but none found"))
    }

    /**
     * Request should fail request validation due to nonconformant request body.
     */
    @Test
    fun testInvalidRequestBody(testContext: TestContext?) {
        RestAssured.given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .`when`()
            .header("X-CorrelationID", "foo")
            .body("{ \"invalid\": \"request\" }")
            .post("/pets")
            .then()
            .log().ifValidationFails()
            .statusCode(400)
            .body(
                Matchers.containsString("Object instance has properties which are not allowed by the schema"),
                Matchers.containsString("Object has missing required properties ([\"id\",\"name\"])")
            )
    }

    /**
     * Request should fail request validation due to missing header.
     */
    @Test
    fun testMissingHeader(testContext: TestContext?) {
        RestAssured.given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .`when`()
            .body("{ \"id\": 1, \"name\": \"Cat\" }")
            .post("/pets")
            .then()
            .log().ifValidationFails()
            .statusCode(400)
            .body(Matchers.containsString("Header parameter 'X-CorrelationID' is required on path '/pets' but not found in request"))
    }

    /**
     * Request should fail request validation due to nonconformant path parameter.
     */
    @Test
    fun testInvalidPathParameter(testContext: TestContext?) {
        RestAssured.given()
            .log().ifValidationFails()
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .`when`()
            .body("{ \"id\": 1, \"name\": \"Cat\" }")
            .put("/pets/invalidId")
            .then()
            .log().ifValidationFails()
            .statusCode(400)
            .body(Matchers.containsString("Instance type (string) does not match any allowed primitive type (allowed: [\"integer\"])"))
    }

    /**
     * Request should fail request validation due to missing request parameter.
     */
    @Test
    fun testMissingRequestParameter(testContext: TestContext?) {
        RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.JSON)
            .`when`().get("/vets")
            .then()
            .log().ifValidationFails()
            .statusCode(400)
            .body(Matchers.containsString("Query parameter 'limit' is required on path '/vets' but not found in request"))
    }

    /**
     * Request should pass request validation due to provided request parameter.
     */
    @Test
    fun testValidRequestParameter(testContext: TestContext?) {
        RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.JSON)
            .`when`().get("/vets?limit=1")
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body("$", Matchers.hasSize<Any>(1))
            .body(
                "$", Matchers.hasItem(
                    Matchers.hasEntry("name", "SupaVets")
                )
            )
    }

    /**
     * Request should pass request validation due to provided request parameter.
     */
    @Test
    fun testPathParamsNotTreatedAsQueryParams(testContext: TestContext?) {
        RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.JSON)
            .`when`().get("/pets/10/status")
            .then()
            .log().ifValidationFails()
            .statusCode(200)
            .body(
                "$",
                Matchers.allOf(
                    Matchers.hasEntry("id", 0),
                    Matchers.hasEntry("valid", false)
                )
            )
    }

    /**
     * Request should fail request validation due to missing request parameter.
     */
    @Test
    fun testInvalidExtraQueryParam(testContext: TestContext?) {
        RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.JSON)
            .queryParam("foo", "bar")
            .`when`().get("/pets/10/status")
            .then()
            .log().ifValidationFails()
            .statusCode(400)
            .body(Matchers.containsString("Query parameter 'foo' is unexpected on path \"/pets/{petId}/status"))
    }
}
