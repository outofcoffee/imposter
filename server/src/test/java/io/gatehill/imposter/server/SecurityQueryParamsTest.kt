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
 * Tests for security using query parameters.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class SecurityQueryParamsTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/security-config-query-params"
    )

    /**
     * Deny - no authentication provided.
     */
    @Test
    fun testRequestDenied_NoAuth() {
        RestAssured.given().`when`()["/example"]
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_UNAUTHORIZED))
    }

    /**
     * Deny - the 'Permit' condition does not match.
     */
    @Test
    fun testRequestDenied_NoPermitMatch() {
        RestAssured.given().`when`()
            .queryParam("apiKey", "invalid-value")
            .queryParam("userKey", "opensesame")["/example"]
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_UNAUTHORIZED))
    }

    /**
     * Deny - the 'Deny' condition matches.
     */
    @Test
    fun testRequestDenied_DenyMatch() {
        RestAssured.given().`when`()
            .queryParam("apiKey", "s3cr3t")
            .queryParam("userKey", "does-not-match")["/example"]
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_UNAUTHORIZED))
    }

    /**
     * Deny - only one condition satisfied.
     */
    @Test
    fun testRequestDenied_OnlyOneMatch() {
        RestAssured.given().`when`()
            .queryParam("apiKey", "s3cr3t")["/example"]
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_UNAUTHORIZED))
        RestAssured.given().`when`()
            .queryParam("userKey", "opensesame")["/example"]
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_UNAUTHORIZED))
    }

    /**
     * Permit - both conditions are satisfied.
     */
    @Test
    fun testRequestPermitted() {
        RestAssured.given().`when`()
            .queryParam("apiKey", "s3cr3t")
            .queryParam("userKey", "opensesame")["/example"]
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
    }
}