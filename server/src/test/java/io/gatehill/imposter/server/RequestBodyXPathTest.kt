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
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for matching request body with XPath.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class RequestBodyXPathTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/request-body-xpath"
    )

    /**
     * Match against a string in the request body.
     */
    @Test
    fun testMatchStringInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.XML)
            .body("""
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/"> 
  <env:Header/>
  <env:Body>
    <pets:animal xmlns:pets="urn:com:example:petstore">
      <pets:name>Fluffy</pets:name>
    </pets:animal>
  </env:Body>
</env:Envelope>
""".trim())
            .post("/example")
            .then()
            .statusCode(equalTo(204))
    }

    /**
     * Negative match against a string in the request body.
     */
    @Test
    fun testNegativeMatchStringInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.XML)
            .body("""
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/"> 
  <env:Header/>
  <env:Body>
    <pets:animal xmlns:pets="urn:com:example:petstore">
      <pets:id>3</pets:id>
      <pets:name>Fluffy</pets:name>
    </pets:animal>
  </env:Body>
</env:Envelope>
""".trim())
            .post("/example-negative")
            .then()
            .body(equalTo("NotEqualTo"))
    }

    /**
     * Match against an integer in the request body.
     */
    @Test
    fun testMatchIntegerInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.XML)
            .body("""
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/"> 
  <env:Header/>
  <env:Body>
    <pets:animal xmlns:pets="urn:com:example:petstore">
      <pets:id>3</pets:id>
    </pets:animal>
  </env:Body>
</env:Envelope>
""".trim())
            .post("/example")
            .then()
            .statusCode(equalTo(302))
    }

    /**
     * Match null against an empty XPath result in the request body.
     */
    @Test
    fun testMatchNullRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.XML)
            .body("""
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/"> 
  <env:Header/>
  <env:Body>
    <pets:animal xmlns:pets="urn:com:example:petstore">
      <pets:id>3</pets:id>
      <pets:name>Fluffy</pets:name>
    </pets:animal>
  </env:Body>
</env:Envelope>
""".trim())
            .post("/example-nonmatch")
            .then()
            .statusCode(equalTo(409))
    }

    /**
     * Match when a given node exists.
     */
    @Test
    fun testMatchNodeExists() {
        RestAssured.given().`when`()
            .contentType(ContentType.XML)
            .body(
                """
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/"> 
  <env:Header/>
  <env:Body>
    <pets:animal xmlns:pets="urn:com:example:petstore">
      <pets:id>3</pets:id>
      <pets:name>Fluffy</pets:name>
    </pets:animal>
  </env:Body>
</env:Envelope>
""".trim()
            )
            .post("/example-exists")
            .then()
            .statusCode(equalTo(201))
    }

    /**
     * Match when a given node does not exist.
     */
    @Test
    fun testMatchNodeNotExists() {
        RestAssured.given().`when`()
            .contentType(ContentType.XML)
            .body("""
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/"> 
  <env:Header/>
  <env:Body>
    <pets:animal xmlns:pets="urn:com:example:petstore">
      <pets:id>3</pets:id>
      <pets:name>Fluffy</pets:name>
    </pets:animal>
  </env:Body>
</env:Envelope>
""".trim())
            .post("/example-not-exists")
            .then()
            .statusCode(equalTo(202))
    }

    /**
     * Match when a string in the request body contains a given value.
     */
    @Test
    fun testMatchStringContainsInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.XML)
            .body("""
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/"> 
  <env:Header/>
  <env:Body>
    <pets:animal xmlns:pets="urn:com:example:petstore">
      <pets:id>3</pets:id>
      <pets:name>Fluffy</pets:name>
    </pets:animal>
  </env:Body>
</env:Envelope>
""".trim())
            .post("/example-contains")
            .then()
            .body(equalTo("Contains"))
    }

    /**
     * Match when a string in the request body contains a given value.
     */
    @Test
    fun testNegativeMatchStringContainsInRequestBody() {
        RestAssured.given().`when`()
            .contentType(ContentType.XML)
            .body("""
<?xml version="1.0" encoding="UTF-8"?>
<env:Envelope xmlns:env="http://schemas.xmlsoap.org/soap/envelope/"> 
  <env:Header/>
  <env:Body>
    <pets:animal xmlns:pets="urn:com:example:petstore">
      <pets:id>3</pets:id>
      <pets:name>Paws</pets:name>
    </pets:animal>
  </env:Body>
</env:Envelope>
""".trim())
            .post("/example-contains-negative")
            .then()
            .body(equalTo("NotContains"))
    }
}
