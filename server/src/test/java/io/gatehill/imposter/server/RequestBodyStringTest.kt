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
package io.gatehill.imposter.server

import io.gatehill.imposter.plugin.test.TestPluginImpl
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasLength
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for matching request body with JsonPath.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class RequestBodyStringTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/request-body-string"
    )

    /**
     * Match against a string in the request body.
     */
    @Test
    fun testMatchStringInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("bar")
            .post("/example")
            .then()
            .statusCode(equalTo(204))
    }

    /**
     * Negative match against a string in the request body.
     */
    @Test
    fun testNegativeMatchStringInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("bar")
            .post("/example-negative")
            .then()
            .body(equalTo("NotEqualTo"))
    }

    /**
     * Match against an integer in the request body.
     */
    @Test
    fun testMatchIntegerInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("99")
            .post("/example")
            .then()
            .statusCode(equalTo(302))
    }

    /**
     * Match null against an empty JsonPath result in the request body.
     */
    @Test
    fun testMatchNullRequestBody() {
        // no body sent
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .post("/example-nonmatch")
            .then()
            .statusCode(equalTo(409))
    }

    /**
     * Match when a given node exists.
     */
    @Test
    fun testMatchNodeExists() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("bar")
            .post("/example-exists")
            .then()
            .statusCode(equalTo(201))
    }

    /**
     * Match when a given node does not exist.
     */
    @Test
    fun testMatchNodeNotExists() {
        // no body sent
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .post("/example-not-exists")
            .then()
            .statusCode(equalTo(202))
    }

    /**
     * Match when a string in the request body contains a given value.
     */
    @Test
    fun testMatchStringContainsInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("bar")
            .post("/example-contains")
            .then()
            .body(equalTo("Contains"))
    }

    /**
     * Negative match when a string in the request body contains a given value.
     */
    @Test
    fun testNegativeMatchStringContainsInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("qux")
            .post("/example-contains-negative")
            .then()
            .body(equalTo("NotContains"))
    }

    /**
     * Regex match on a string in the request body.
     */
    @Test
    fun testMatchStringRegexInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("bar")
            .post("/example-regex")
            .then()
            .body(equalTo("Matches"))
    }

    /**
     * Negative regex match on a string in the request body.
     */
    @Test
    fun testNegativeMatchStringRegexInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("qux")
            .post("/example-regex-negative")
            .then()
            .body(equalTo("NotMatches"))
    }

    /**
     * Match requiring multiple strings in the request body.
     */
    @Test
    fun testAllOfMatchInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("bar qux")
            .post("/example-allof")
            .then()
            .body(equalTo("AllOf"))
    }

    /**
     * Unsatisfied match requiring multiple strings in the request body.
     */
    @Test
    fun testUnsatisfiedAllOfMatchInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("bar no match")
            .post("/example-allof")
            .then()
            .body(hasLength(0))
    }

    /**
     * Match requiring any of an array of strings in the request body.
     */
    @Test
    fun testAnyOfMatchInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.TEXT)
            .body("bar")
            .post("/example-anyof")
            .then()
            .body(equalTo("AnyOf"))
    }
}
