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
package io.gatehill.imposter.server

import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.plugin.config.security.SecurityCondition
import io.gatehill.imposter.plugin.config.security.SecurityEffect
import io.gatehill.imposter.plugin.test.TestPluginImpl
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.InjectorUtil
import io.restassured.RestAssured
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class SecurityConfigTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/security-config"
    )

    @Test
    fun testPluginLoadAndConfig(testContext: TestContext) {
        val pluginManager = InjectorUtil.getInstance<PluginManager>()
        val plugin = pluginManager.getPlugin<TestPluginImpl>(TestPluginImpl::class.java.canonicalName)
        testContext.assertNotNull(plugin)
        testContext.assertNotNull(plugin!!.configs)
        testContext.assertEquals(1, plugin.configs.size)
        val pluginConfig = plugin.configs[0]

        // check security config
        val securityConfig = pluginConfig.securityConfig
        testContext.assertNotNull(securityConfig)
        testContext.assertEquals(SecurityEffect.Deny, securityConfig!!.defaultEffect)

        // check conditions
        testContext.assertEquals(2, securityConfig.conditions.size)

        // check short configuration option
        val condition1 = securityConfig.conditions[0]
        testContext.assertEquals(SecurityEffect.Permit, condition1.effect)
        val parsedHeaders1 = SecurityCondition.requestHeaders(condition1)
        testContext.assertEquals(1, parsedHeaders1.size)
        testContext.assertEquals("s3cr3t", parsedHeaders1["Authorization"]!!.value)

        // check long configuration option
        val condition2 = securityConfig.conditions[1]
        testContext.assertEquals(SecurityEffect.Deny, condition2.effect)
        val parsedHeaders2 = SecurityCondition.requestHeaders(condition2)
        testContext.assertEquals(1, parsedHeaders2.size)
        testContext.assertEquals("opensesame", parsedHeaders2["X-Api-Key"]!!.value)
    }

    /**
     * Deny - no authentication provided.
     */
    @Test
    fun testRequestDenied_NoAuth() {
        RestAssured.given().`when`()
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_UNAUTHORIZED))
    }

    /**
     * Deny - the 'Permit' condition does not match.
     */
    @Test
    fun testRequestDenied_NoPermitMatch() {
        RestAssured.given().`when`()
            .header("Authorization", "invalid-value")
            .header("X-Api-Key", "opensesame")
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_UNAUTHORIZED))
    }

    /**
     * Deny - the 'Deny' condition matches.
     */
    @Test
    fun testRequestDenied_DenyMatch() {
        RestAssured.given().`when`()
            .header("Authorization", "s3cr3t")
            .header("X-Api-Key", "does-not-match")
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_UNAUTHORIZED))
    }

    /**
     * Deny - only one condition satisfied.
     */
    @Test
    fun testRequestDenied_OnlyOneMatch() {
        RestAssured.given().`when`()
            .header("Authorization", "s3cr3t")
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_UNAUTHORIZED))
        RestAssured.given().`when`()
            .header("X-Api-Key", "opensesame")
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_UNAUTHORIZED))
    }

    /**
     * Permit - both conditions are satisfied.
     */
    @Test
    fun testResourceRequestPermitted() {
        RestAssured.given().`when`()
            .header("Authorization", "s3cr3t")
            .header("X-Api-Key", "opensesame")
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
    }

    /**
     * Permit - both conditions are satisfied, even though the case of the header
     * name differs from that in the configuration.
     */
    @Test
    fun testResourceRequestPermitted_CaseInsensitive() {
        RestAssured.given().`when`()
            .header("authorization", "s3cr3t")
            .header("x-api-key", "opensesame")
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
        RestAssured.given().`when`()
            .header("AUTHORIZATION", "s3cr3t")
            .header("X-API-KEY", "opensesame")
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
    }

    /**
     * Permit - status endpoint is explicitly permitted.
     */
    @Test
    fun testStatusRequestPermitted() {
        RestAssured.given().`when`()
            .get("/system/status")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
    }
}
