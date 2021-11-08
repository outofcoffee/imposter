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
package io.gatehill.imposter.plugin.openapi

import com.jayway.restassured.RestAssured
import com.jayway.restassured.http.ContentType
import io.gatehill.imposter.server.BaseVerticleTest
import io.gatehill.imposter.util.HttpUtil
import io.vertx.ext.unit.TestContext
import org.junit.Before
import org.junit.Test
import org.yaml.snakeyaml.Yaml

/**
 * Tests for OpenAPI definitions with object examples.
 *
 * @author Pete Cornish
 */
class ObjectExamplesTest : BaseVerticleTest() {
    override val pluginClass = OpenApiPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/openapi2/object-examples"
    )

    /**
     * Should return object example formatted as JSON.
     */
    @Test
    fun testObjectExampleAsJson(testContext: TestContext) {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(ContentType.JSON)
            .`when`()["/objects/team"]
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .extract().jsonPath()

        testContext.assertEquals(10, body.get("id"))
        testContext.assertEquals("Engineering", body.get("name"))
    }

    /**
     * Should return object example formatted as YAML.
     */
    @Test
    fun testObjectExampleAsYaml(testContext: TestContext) {
        val rawBody = RestAssured.given()
            .log().ifValidationFails()
            .accept("application/x-yaml")
            .`when`()["/objects/team"]
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .extract().asString()

        val yamlBody = YAML_PARSER.load<Map<String, *>>(rawBody)
        testContext.assertEquals(20, yamlBody["id"])
        testContext.assertEquals("Product", yamlBody["name"])
    }

    companion object {
        private val YAML_PARSER = Yaml()
    }
}