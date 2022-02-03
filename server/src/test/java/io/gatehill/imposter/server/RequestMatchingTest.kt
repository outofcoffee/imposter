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

import com.jayway.restassured.RestAssured
import io.gatehill.imposter.plugin.test.TestPluginImpl
import io.gatehill.imposter.util.HttpUtil
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for matching path parameters.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class RequestMatchingTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/request-matching"
    )

    /**
     * Match against a path parameter defined in configuration in Vert.x format.
     */
    @Test
    fun testMatchPathParamVertxFormat() {
        RestAssured.given().`when`()
            .get("/users/1")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_NO_CONTENT))
    }

    /**
     * Match against a path parameter defined in configuration in OpenAPI format.
     */
    @Test
    fun testMatchPathParamOpenApiFormat() {
        RestAssured.given().`when`()
            .get("/orders/99")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_NOT_AUTHORITATIVE))
    }

    /**
     * Match against a path with no HTTP method.
     */
    @Test
    fun testMatchPathNoMethod() {
        RestAssured.given().`when`()
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_ACCEPTED))

        RestAssured.given().`when`()
            .post("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_ACCEPTED))

        RestAssured.given().`when`()
            .put("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_ACCEPTED))

        RestAssured.given().`when`()
            .patch("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_ACCEPTED))

        RestAssured.given().`when`()
            .delete("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_ACCEPTED))

        RestAssured.given().`when`()
            .head("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_ACCEPTED))
    }
}
