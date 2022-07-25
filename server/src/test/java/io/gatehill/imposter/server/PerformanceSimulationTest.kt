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
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for performance simulation.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class PerformanceSimulationTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/performance-simulation"
    )

    /**
     * The response should have a latency of at least 500ms.
     */
    @Test
    fun testRequestDelayed_StaticExact() {
        val startMs = System.currentTimeMillis()
        RestAssured.given().`when`()
            .get("/static-exact-delay")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
        val latency = System.currentTimeMillis() - startMs
        Assert.assertTrue(
            "Response latency should be >= 500ms - was: $latency",
            latency >= 500
        )
    }

    /**
     * The response should have a latency of roughly between 200ms-400ms,
     * plus the [MEASUREMENT_TOLERANCE].
     */
    @Test
    fun testRequestDelayed_StaticRange() {
        val startMs = System.currentTimeMillis()
        RestAssured.given().`when`()
            .get("/static-range-delay")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
        val latency = System.currentTimeMillis() - startMs
        Assert.assertTrue(
            "Response latency should be >= 200ms and <= 400ms - was: $latency",
            latency >= 200 && latency <= 400 + MEASUREMENT_TOLERANCE
        )
    }

    /**
     * The response should have a latency of at least 500ms.
     */
    @Test
    fun testRequestDelayed_ScriptedExact() {
        val startMs = System.currentTimeMillis()
        RestAssured.given().`when`()
            .get("/scripted-exact-delay")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
        val latency = System.currentTimeMillis() - startMs
        Assert.assertTrue(
            "Response latency should be >= 500ms - was: $latency",
            latency >= 500
        )
    }

    /**
     * The response should have a latency of roughly between 200ms-400ms,
     * plus the [MEASUREMENT_TOLERANCE].
     */
    @Test
    fun testRequestDelayed_ScriptedRange() {
        val startMs = System.currentTimeMillis()
        RestAssured.given().`when`()
            .get("/scripted-range-delay")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
        val latency = System.currentTimeMillis() - startMs
        Assert.assertTrue(
            "Response latency should be >= 200ms and <= 400ms - was: $latency",
            latency >= 200 && latency <= 400 + MEASUREMENT_TOLERANCE
        )
    }

    companion object {
        /**
         * Tolerate (very) slow test execution conditions.
         */
        private const val MEASUREMENT_TOLERANCE = 2000
    }
}