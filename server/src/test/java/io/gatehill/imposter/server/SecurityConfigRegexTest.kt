/*
 * Copyright (c) 2023-2023.
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
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.apache.commons.lang3.RandomStringUtils
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for security configuration using regular expressions.
 */
class SecurityConfigRegexTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/security-config-regex"
    )

    /**
     * Permit - request is permitted via regex.
     */
    @Test
    fun testRegexMatchesRequestPermitted() {
        RestAssured.given().`when`()
            .header("Authorization", "Bearer " + RandomStringUtils.random(50, "ABCDEFGHIJKLMNOPRSTUVXYZabcdefghijklmnoprstuvxyz1234567890"))
            .get("/match")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
    }

    /**
     * Deny - request is denied because of not matching regex
     */
    @Test
    fun testRegexMatchesRequestDeny() {
        RestAssured.given().`when`()
            .header("Authorization", "Token ")
            .get("/match")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED))
    }

    /**
     * Permit - request is denied because of matching regex
     */
    @Test
    fun testRegexNotMatchesRequestPermit() {
        RestAssured.given().`when`()
            .header("Authorization", "Bearer magic-token")
            .get("/does-not-match")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
    }

    /**
     * Deny - request is denied because of matching regex
     */
    @Test
    fun testRegexNotMatchesRequestDeny() {
        RestAssured.given().`when`()
            .header("Authorization", "Bearer bad-token")
            .get("/does-not-match")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED))
    }
}
