/*
 * Copyright (c) 2016-2022.
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
package io.gatehill.imposter.plugin.openapi

import io.gatehill.imposter.server.BaseVerticleTest
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.hamcrest.Matchers
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for operation base path for OAS2 specification.
 *
 * @author Pete Cornish
 */
class OAS2BasePathTest : BaseVerticleTest() {
    override val pluginClass = OpenApiPluginImpl::class.java

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/openapi2/base-path"
    )

    @Test
    fun `spec base path applied`() {
        // the server path ('/petstore') in the spec should be stripped
        RestAssured.given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/api/example")
            .then()
            .statusCode(HttpUtil.HTTP_BAD_REQUEST)
    }

    @Test
    fun `paths not rewritten in spec`() {
        RestAssured.given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/_spec/combined.json")
            .then()
            .statusCode(HttpUtil.HTTP_OK)
            .body("paths.'/example'", notNullValue())
    }

    @Test
    fun `server added in spec`() {
        RestAssured.given()
            .accept(ContentType.JSON)
            .`when`()
            .get("/_spec/combined.json")
            .then()
            .statusCode(HttpUtil.HTTP_OK)
            .body("servers", Matchers.allOf(
                Matchers.hasItem(Matchers.hasEntry("url", "/api")),
                Matchers.hasItem(Matchers.hasEntry("url", "http://$host:$listenPort/api"))
            ))
    }
}
