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
package io.gatehill.imposter.plugin.soap

import io.gatehill.imposter.plugin.soap.util.SoapUtil
import io.gatehill.imposter.server.BaseVerticleTest
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SoapPluginImpl] generation of fault responses.
 *
 * @author Pete Cornish
 */
class FaultExampleTest : BaseVerticleTest() {
    override val pluginClass = SoapPluginImpl::class.java
    override val testConfigDirs = listOf("/fault-example")
    private val soapEnvNamespace = SoapUtil.soap12RecEnvNamespace
    private val soapContentType = SoapUtil.soap12ContentType

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    @Test
    fun `respond with a fault generated from the schema if status is 500`() {
        val getPetByIdEnv = SoapUtil.wrapInEnv(
            """
<getPetByIdRequest xmlns="urn:com:example:petstore">
  <id>10</id>
</getPetByIdRequest>
""".trim(), soapEnvNamespace
        )

        RestAssured.given()
            .log().ifValidationFails()
            .accept(soapContentType)
            .contentType(soapContentType)
            .`when`()
            .body(getPetByIdEnv)
            .post("/pets/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_INTERNAL_ERROR)
            .body(
                Matchers.allOf(
                    Matchers.containsString("Envelope"),
                    Matchers.containsString("getPetFault"),
                    Matchers.containsString("code"),
                    Matchers.containsString("description"),
                )
            )
    }

    @Test
    fun `respond with a custom fault if status is 500 and content set`() {
        val getPetByIdEnv = SoapUtil.wrapInEnv(
            """
<getPetByIdRequest xmlns="urn:com:example:petstore">
  <id>2</id>
</getPetByIdRequest>
""".trim(), soapEnvNamespace
        )

        RestAssured.given()
            .log().ifValidationFails()
            .accept(soapContentType)
            .contentType(soapContentType)
            .`when`()
            .body(getPetByIdEnv)
            .post("/pets/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_INTERNAL_ERROR)
            .body(
                Matchers.allOf(
                    Matchers.containsString("Envelope"),
                    Matchers.containsString("soap:Fault"),
                    Matchers.containsString("code"),
                    Matchers.containsString("description"),
                    Matchers.containsString("Custom fault"),
                )
            )
    }

    @Test
    fun `respond with a fault generated from the schema if response configuration set`() {
        val getPetByIdEnv = SoapUtil.wrapInEnv(
            """
<getPetByIdRequest xmlns="urn:com:example:petstore">
  <id>99</id>
</getPetByIdRequest>
""".trim(), soapEnvNamespace
        )

        RestAssured.given()
            .log().ifValidationFails()
            .accept(soapContentType)
            .contentType(soapContentType)
            .`when`()
            .body(getPetByIdEnv)
            .post("/pets/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_INTERNAL_ERROR)
            .body(
                Matchers.allOf(
                    Matchers.containsString("Envelope"),
                    Matchers.containsString("getPetFault"),
                    Matchers.containsString("code"),
                    Matchers.containsString("description"),
                )
            )
    }

    @Test
    fun `respond with a fault generated from the schema if script function called`() {
        val getPetByIdEnv = SoapUtil.wrapInEnv(
            """
<getPetByIdRequest xmlns="urn:com:example:petstore">
  <id>100</id>
</getPetByIdRequest>
""".trim(), soapEnvNamespace
        )

        RestAssured.given()
            .log().ifValidationFails()
            .accept(soapContentType)
            .contentType(soapContentType)
            .`when`()
            .body(getPetByIdEnv)
            .post("/pets/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_INTERNAL_ERROR)
            .body(
                Matchers.allOf(
                    Matchers.containsString("Envelope"),
                    Matchers.containsString("getPetFault"),
                    Matchers.containsString("code"),
                    Matchers.containsString("description"),
                )
            )
    }
}
