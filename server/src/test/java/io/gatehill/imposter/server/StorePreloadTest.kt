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
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for preloading data into storage subsystem.
 *
 * @author Pete Cornish
 */
class StorePreloadTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @BeforeEach
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/store-preload"
    )

    /**
     * Fetch data preloaded from a file.
     */
    @Test
    fun testFetchPreloadedFile() {
        // simple value
        RestAssured.given().`when`()
            .pathParam("storeId", "preload-file-test")
            .pathParam("key", "foo")["/system/store/{storeId}/{key}"]
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("bar"))

        // object value
        RestAssured.given().`when`()
            .pathParam("storeId", "preload-file-test")
            .pathParam("key", "baz")["/system/store/{storeId}/{key}"]
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body("qux", Matchers.equalTo("corge"))
    }

    /**
     * Fetch data preloaded from inline config.
     */
    @Test
    fun testFetchPreloadedInlineData() {
        // simple value
        RestAssured.given().`when`()
            .pathParam("storeId", "preload-data-test")
            .pathParam("key", "foo")["/system/store/{storeId}/{key}"]
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("bar"))

        // object value
        RestAssured.given().`when`()
            .pathParam("storeId", "preload-data-test")
            .pathParam("key", "baz")["/system/store/{storeId}/{key}"]
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body("qux", Matchers.equalTo("corge"))
    }
}