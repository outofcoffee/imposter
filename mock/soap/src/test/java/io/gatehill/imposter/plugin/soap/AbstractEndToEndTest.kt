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
package io.gatehill.imposter.plugin.soap

import io.gatehill.imposter.plugin.soap.util.SoapUtil
import io.gatehill.imposter.server.BaseVerticleTest
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.jdom2.Namespace
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Common tests for [SoapPluginImpl].
 *
 * @author Pete Cornish
 */
abstract class AbstractEndToEndTest : BaseVerticleTest() {
    override val pluginClass = SoapPluginImpl::class.java

    protected abstract val soapEnvNamespace: Namespace
    protected abstract val soapContentType: String

    protected open val getPetByIdEnv
        get() = SoapUtil.wrapInEnv(
            """
<getPetByIdRequest xmlns="urn:com:example:petstore">
  <id>3</id>
</getPetByIdRequest>
""".trim(), soapEnvNamespace
        )

    protected open val getPetByNameEnv
        get() = SoapUtil.wrapInEnv(
            """
<getPetByNameRequest xmlns="urn:com:example:petstore">
  <name>Fluffy</name>
</getPetByNameRequest>
""".trim(), soapEnvNamespace
    )

    @BeforeEach
    @Throws(Exception::class)
    override fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        super.setUp(vertx, testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
    }

    @Test
    fun testRequestResponseUsingSoapActionHeader() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(soapContentType)
            .contentType(soapContentType)
            .header("SOAPAction", "getPetById")
            .`when`()
            .body(getPetByIdEnv)
            .post("/soap/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .body(containsString("getPetByIdResponse"))
            .extract().asString()

        assertNotNull(body)
    }

    protected open fun soap12RequestResponseUsingSoapActionInContentType() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(soapContentType)
            .contentType("$soapContentType;charset=UTF-8;action=\"getPetById\"")
            .`when`()
            .body(getPetByIdEnv)
            .post("/soap/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .body(containsString("getPetByIdResponse"))
            .extract().asString()

        assertNotNull(body)
    }

    @Test
    fun testRequestResponseUsingRequestBody() {
        val body = RestAssured.given()
            .log().ifValidationFails()
            .accept(soapContentType)
            .contentType(soapContentType)
            .`when`()
            .body(getPetByIdEnv)
            .post("/soap/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .body(containsString("getPetByIdResponse"))
            .extract().asString()

        assertNotNull(body)
    }

    @Test
    fun test404OnInvalidSoapAction() {
        RestAssured.given()
            .log().ifValidationFails()
            .accept(soapContentType)
            .contentType(soapContentType)
            .header("SOAPAction", "invalid")
            .`when`()
            .body(getPetByIdEnv)
            .post("/soap/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_NOT_FOUND)
    }

    @Test
    fun testBindingAndOperationMatch() {
        RestAssured.given()
            .log().ifValidationFails()
            .accept(soapContentType)
            .contentType(soapContentType)
            .`when`()
            .body(getPetByNameEnv)
            .post("/soap/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .body(
                allOf(
                    containsString("Envelope"),
                    containsString("getPetByNameResponse"),
                    containsString("Fluffy"),
                )
            )
    }

    @Test
    fun testHttpBinding() {
        RestAssured.given()
            .log().ifValidationFails()
            .accept(soapContentType)
            .contentType(soapContentType)
            .`when`()
            .body(getPetByNameEnv)
            .post("/http/")
            .then()
            .log().ifValidationFails()
            .statusCode(HttpUtil.HTTP_OK)
            .body(
                allOf(
                    containsString("Envelope"),
                    containsString("getPetByNameResponse"),
                    containsString("Fluffy"),
                )
            )
    }
}
