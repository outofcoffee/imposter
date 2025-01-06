/*
 * Copyright (c) 2016-2024.
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
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for expression matcher functionality.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class ExpressionMatcherTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/expression-matcher"
    )

    /**
     * Match against a header value using eval.
     */
    @Test
    fun testMatchHeaderUsingEval() {
        RestAssured.given().`when`()
            .header("X-Test", "test-value")
            .post("/example")
            .then()
            .statusCode(equalTo(204))
    }

    /**
     * Negative match against a header value using eval.
     */
    @Test
    fun testNegativeMatchHeaderUsingEval() {
        RestAssured.given().`when`()
            .header("X-Test", "test-value")
            .post("/example-negative")
            .then()
            .body(equalTo("NotEqualTo"))
    }

    /**
     * Match when a header value contains a given value using eval.
     */
    @Test
    fun testMatchHeaderContainsUsingEval() {
        RestAssured.given().`when`()
            .header("X-Test", "test-value")
            .post("/example-contains")
            .then()
            .body(equalTo("Contains"))
    }

    /**
     * Negative match when a header value contains a given value using eval.
     */
    @Test
    fun testNegativeMatchHeaderContainsUsingEval() {
        RestAssured.given().`when`()
            .header("X-Test", "test-value")
            .post("/example-contains-negative")
            .then()
            .body(equalTo("NotContains"))
    }

    /**
     * Regex match on a header value using eval.
     */
    @Test
    fun testMatchHeaderRegexUsingEval() {
        RestAssured.given().`when`()
            .header("X-Test", "test-value")
            .post("/example-regex")
            .then()
            .body(equalTo("Matches"))
    }

    /**
     * Negative regex match on a header value using eval.
     */
    @Test
    fun testNegativeMatchHeaderRegexUsingEval() {
        RestAssured.given().`when`()
            .header("X-Test", "wrong-value")
            .post("/example-regex-negative")
            .then()
            .body(equalTo("NotMatches"))
    }

    /**
     * Match requiring multiple header values using allOf.
     */
    @Test
    fun testAllOfMatchHeadersUsingEval() {
        RestAssured.given().`when`()
            .header("X-Test1", "test-value-1")
            .header("X-Test2", "test-value-2")
            .post("/example-allof")
            .then()
            .body(equalTo("AllOf"))
    }

    /**
     * Match requiring any of header values using anyOf - first header matches.
     */
    @Test
    fun testAnyOfMatchHeadersFirstMatches() {
        RestAssured.given().`when`()
            .header("X-Test1", "test-value-1")
            .header("X-Test2", "wrong-value")
            .post("/example-anyof-match-first")
            .then()
            .body(equalTo("AnyOf"))
    }

    /**
     * Match requiring any of header values using anyOf - second header matches.
     */
    @Test
    fun testAnyOfMatchHeadersSecondMatches() {
        RestAssured.given().`when`()
            .header("X-Test1", "wrong-value")
            .header("X-Test2", "test-value-2")
            .post("/example-anyof-match-second")
            .then()
            .body(equalTo("AnyOf"))
    }

    /**
     * Match requiring any of header values using anyOf - no headers match.
     */
    @Test
    fun testAnyOfMatchHeadersNoMatch() {
        RestAssured.given().`when`()
            .header("X-Test1", "wrong-value-1")
            .header("X-Test2", "wrong-value-2")
            .post("/example-anyof-no-match")
            .then()
            .statusCode(equalTo(404))
    }
}
