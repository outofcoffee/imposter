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
import com.jayway.restassured.http.ContentType
import io.gatehill.imposter.plugin.test.TestPluginImpl
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.attempt
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.apache.commons.io.FileUtils
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Tests for item capture.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class CaptureTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/capture"
    )

    /**
     * Capture request header attributes into a store.
     */
    @Test
    fun testCaptureHeaderItems() {
        // send data for capture
        RestAssured.given().`when`()
            .pathParam("userId", "foo")
            .queryParam("page", 2)
            .header("X-Correlation-ID", "abc123")
            .get("/users/{userId}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))

        // retrieve via system
        RestAssured.given().`when`()
            .pathParam("storeId", "captureTest")
            .pathParam("key", "userId")
            .get("/system/store/{storeId}/{key}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("foo"))

        RestAssured.given().`when`()
            .pathParam("storeId", "captureTest")
            .pathParam("key", "page")
            .get("/system/store/{storeId}/{key}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("2"))

        RestAssured.given().`when`()
            .pathParam("storeId", "captureTest")
            .pathParam("key", "correlationId")
            .get("/system/store/{storeId}/{key}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("abc123"))
    }

    /**
     * Capture request body properties into a store.
     */
    @Test
    @Throws(Exception::class)
    fun testCaptureBodyItems() {
        val users = FileUtils.readFileToString(
            File(CaptureTest::class.java.getResource("/capture/user.json").toURI()),
            StandardCharsets.UTF_8
        )

        // send data for capture
        RestAssured.given().`when`()
            .body(users)
            .contentType(ContentType.JSON)
            .post("/users")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))

        // retrieve via system
        RestAssured.given().`when`()
            .pathParam("storeId", "captureTest")
            .pathParam("key", "name")
            .get("/system/store/{storeId}/{key}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("Alice"))

        RestAssured.given().`when`()
            .pathParam("storeId", "captureTest")
            .pathParam("key", "postCode")
            .get("/system/store/{storeId}/{key}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(Matchers.equalTo("PO5 7CO"))

        // the capture configuration for 'street' is disabled, so it
        // should not exist in the store.
        RestAssured.given().`when`()
            .pathParam("storeId", "captureTest")
            .pathParam("key", "street")
            .get("/system/store/{storeId}/{key}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_NOT_FOUND))
    }

    /**
     * Capture a constant value into a store with a dynamic key.
     */
    @Test
    fun testCaptureConstWithDynamicKey() {
        // send data for capture
        RestAssured.given().`when`()
            .pathParam("userId", "alice")
            .put("/users/admins/{userId}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))

        RestAssured.given().`when`()
            .pathParam("userId", "bob")
            .put("/users/admins/{userId}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))

        // retrieve via system
        val body = RestAssured.given().`when`()
            .pathParam("storeId", "captureTestAdmins")
            .get("/system/store/{storeId}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .contentType(ContentType.JSON)
            .extract().body().`as`(MutableMap::class.java)

        MatcherAssert.assertThat(body.entries, Matchers.hasSize(2))
        MatcherAssert.assertThat(
            body, Matchers.allOf(
                Matchers.hasEntry("alice", "admin"),
                Matchers.hasEntry("bob", "admin")
            )
        )
    }

    /**
     * Capture a value using deferred persistence.
     */
    @Test
    fun testCaptureDeferred() {
        // should not exist yet
        RestAssured.given().`when`()
            .pathParam("storeId", "captureDeferred")
            .get("/system/store/{storeId}/userId")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_NOT_FOUND))

        // send data for capture
        RestAssured.given().`when`()
            .pathParam("userId", "alice")
            .put("/defer/{userId}")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_ACCEPTED))

        // allow for background processing to complete
        attempt(attempts = 5) {
            RestAssured.given().`when`()
                .pathParam("storeId", "captureDeferred")
                .get("/system/store/{storeId}/userId")
                .then()
                .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
                .contentType(ContentType.TEXT)
                .body(Matchers.equalTo("alice"))
        }
    }

    /**
     * Capture the response body.
     */
    @Test
    fun testCaptureResponseBody() {
        // should not exist yet
        RestAssured.given().`when`()
            .pathParam("storeId", "captureResponseBody")
            .get("/system/store/{storeId}/userId")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_NOT_FOUND))

        // send data for capture
        RestAssured.given().`when`()
            .get("/response-capture")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))

        // allow for background processing to complete
        attempt(attempts = 5) {
            RestAssured.given().`when`()
                .pathParam("storeId", "captureResponseBody")
                .get("/system/store/{storeId}/responseBody")
                .then()
                .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
                .contentType(ContentType.TEXT)
                .body(Matchers.equalTo("Example response"))
        }
    }
}
