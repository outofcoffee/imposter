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
import io.restassured.RestAssured
import io.restassured.RestAssured.*
import io.restassured.config.RedirectConfig
import io.vertx.ext.unit.TestContext
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * Tests matching using path parameters.

 * @author Pete Cornish
 */
class PathParamsTest : BaseVerticleTest() {
    override val pluginClass = RestPluginImpl::class.java

    override val testConfigDirs = listOf(
        "/path-params"
    )

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        baseURI = "http://$host:$listenPort"
        config().redirect(RedirectConfig.redirectConfig().followRedirects(false))
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @Test
    fun `should match path param with simple form config`() {
        given()
            .`when`()
            .post("/simple/test")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("matched"))
    }

    @Test
    fun `should not match path param with simple form config`() {
        given()
            .`when`()
            .post("/simple/should-not-match")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("not matched"))
    }

    @Test
    fun `should match query params with EqualTo operator`() {
        given()
            .`when`()
            .post("/equalto/test")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("equalto"))
    }

    @Test
    fun `should match query params with NotEqualTo operator`() {
        given()
            .`when`()
            .post("/notequalto/foo")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("notequalto"))
    }

    @Test
    fun `should match query params with Contains operator`() {
        given()
            .`when`()
            .post("/contains/test")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("contains"))
    }

    @Test
    fun `should match query params with NotContains operator`() {
        given()
            .`when`()
            .post("/notcontains/foo")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("notcontains"))
    }

    @Test
    fun `should match query params with Matches operator`() {
        given()
            .`when`()
            .post("/matches/test")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("matches"))
    }

    @Test
    fun `should match query params with NotMatches operator`() {
        given()
            .`when`()
            .post("/notmatches/123")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("notmatches"))
    }

    @Test
    fun `should match query params with Exists operator`() {
        given()
            .`when`()
            .post("/exists/test")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("exists"))
    }

    @Test
    @Ignore("This isn't currently supported")
    fun `should match query params with NotExists operator`() {
        given()
            .`when`()
            .post("/notexists//abc")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .and()
            .body(equalTo("notexists"))
    }
}
