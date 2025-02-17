/*
 * Copyright (c) 2023.
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
package io.gatehill.imposter.plugin.rest

import io.gatehill.imposter.server.BaseVerticleTest
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured.*
import io.restassured.config.RedirectConfig
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests matching using form parameters.

 * @author Pete Cornish
 */
class FormParamsTest : BaseVerticleTest() {
    override val pluginClass = RestPluginImpl::class.java

    override val testConfigDirs = listOf(
        "/form-params"
    )

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        baseURI = "http://$host:$listenPort"
        config().redirect(RedirectConfig.redirectConfig().followRedirects(false))
        enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @Test
    fun `should match form param with simple form config`() {
        given()
            .formParams(mapOf("example" to "test"))
            .`when`()
            .post("/simple")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("matched"))
    }

    @Test
    fun `should not match form param with simple form config`() {
        given()
            .formParams(mapOf("example" to "should-not-match"))
            .`when`()
            .post("/simple")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("not matched"))
    }

    @Test
    fun `should match query params with EqualTo operator`() {
        given()
            .formParam("example", "test")
            .`when`()
            .post("/equalto")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("equalto"))
    }

    @Test
    fun `should match query params with NotEqualTo operator`() {
        given()
            .formParam("example", "foo")
            .`when`()
            .post("/notequalto")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("notequalto"))
    }

    @Test
    fun `should match query params with Contains operator`() {
        given()
            .formParam("example", "test")
            .`when`()
            .post("/contains")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("contains"))
    }

    @Test
    fun `should match query params with NotContains operator`() {
        given()
            .formParam("example", "foo")
            .`when`()
            .post("/notcontains")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("notcontains"))
    }

    @Test
    fun `should match query params with Matches operator`() {
        given()
            .formParam("example", "test")
            .`when`()
            .post("/matches")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("matches"))
    }

    @Test
    fun `should match query params with NotMatches operator`() {
        given()
            .formParam("example", "123")
            .`when`()
            .post("/notmatches")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("notmatches"))
    }

    @Test
    fun `should match query params with Exists operator`() {
        given()
            .formParam("example", "test")
            .`when`()
            .post("/exists")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("exists"))
    }

    @Test
    fun `should match query params with NotExists operator`() {
        given()
            .`when`()
            .post("/notexists")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("notexists"))
    }
}
