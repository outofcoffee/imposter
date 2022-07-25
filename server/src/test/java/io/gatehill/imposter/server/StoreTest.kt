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
import io.restassured.http.ContentType
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.any
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasEntry
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for storage subsystem.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class StoreTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/store"
    )

    /**
     * Save and load from the store across multiple requests.
     */
    @Test
    fun testSetAndGetFromStoreScripted() {
        // save via script
        RestAssured.given().`when`()
            .queryParam("foo", "qux")
            .put("/store")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_CREATED))

        // load via script
        RestAssured.given().`when`()["/load"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .body(equalTo("qux"))
    }

    /**
     * Fail to load a nonexistent store.
     */
    @Test
    fun testNonexistentStore() {
        RestAssured.given().`when`()
            .pathParam("storeId", "nonexistent")["/system/store/{storeId}"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
    }

    /**
     * Fail to load a store with an incorrect Accept header.
     */
    @Test
    fun testUnacceptableMimeType() {
        // populate the store
        RestAssured.given().`when`()
            .pathParam("storeId", "umt")
            .pathParam("key", "foo")
            .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
            .body("baz")
            .put("/system/store/{storeId}/{key}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_CREATED))

        // incorrect mime type
        RestAssured.given().`when`()
            .pathParam("storeId", "umt")
            .accept(ContentType.XML)["/system/store/{storeId}"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_NOT_ACCEPTABLE))

        // correct mime type
        RestAssured.given().`when`()
            .pathParam("storeId", "umt")
            .accept(ContentType.JSON)["/system/store/{storeId}"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
    }

    /**
     * Save and load from the store across multiple requests.
     */
    @Test
    fun testSetAndGetSingleFromStore() {
        // initially empty
        RestAssured.given().`when`()
            .pathParam("storeId", "sgs")
            .pathParam("key", "bar")["/system/store/{storeId}/{key}"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_NOT_FOUND))

        // create via system
        RestAssured.given().`when`()
            .pathParam("storeId", "sgs")
            .pathParam("key", "bar")
            .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
            .body("corge")
            .put("/system/store/{storeId}/{key}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_CREATED))

        // update via system
        RestAssured.given().`when`()
            .pathParam("storeId", "sgs")
            .pathParam("key", "bar")
            .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
            .body("quux")
            .put("/system/store/{storeId}/{key}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))

        // retrieve via system
        RestAssured.given().`when`()
            .pathParam("storeId", "sgs")
            .pathParam("key", "bar")["/system/store/{storeId}/{key}"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .body(equalTo("quux"))
    }

    /**
     * Save and load from the store across multiple requests.
     */
    @Test
    fun testSetAndGetMultipleFromStore() {
        // initially empty
        RestAssured.given().`when`()
            .pathParam("storeId", "sgm")["/system/store/{storeId}"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))

        val body = mapOf(
            "baz" to "quuz",
            "corge" to "grault"
        )

        // save via system
        RestAssured.given().`when`()
            .pathParam("storeId", "sgm")
            .contentType(ContentType.JSON)
            .body(body)
            .post("/system/store/{storeId}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))

        // load all
        RestAssured.given().`when`()
            .pathParam("storeId", "sgm")["/system/store/{storeId}"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .body(
                "$", allOf(
                    hasEntry("baz", "quuz"),
                    hasEntry("corge", "grault")
                )
            )
    }

    /**
     * Delete an item from a store.
     */
    @Test
    fun testDeleteFromStore() {
        // save via system
        RestAssured.given().`when`()
            .pathParam("storeId", "ditem")
            .pathParam("key", "corge")
            .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
            .body("quux")
            .put("/system/store/{storeId}/{key}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_CREATED))

        RestAssured.given().`when`()
            .pathParam("storeId", "ditem")
            .pathParam("key", "corge")
            .delete("/system/store/{storeId}/{key}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT))

        // should not exist
        RestAssured.given().`when`()
            .pathParam("storeId", "ditem")
            .pathParam("key", "corge")["/system/store/{storeId}/{key}"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_NOT_FOUND))
    }

    /**
     * Save and load from the store across multiple requests.
     */
    @Test
    fun testSetAndGetAllFromStore() {
        // save via system
        RestAssured.given().`when`()
            .pathParam("storeId", "sga")
            .pathParam("key", "baz")
            .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
            .body("quuz")
            .put("/system/store/{storeId}/{key}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_CREATED))

        // load all
        RestAssured.given().`when`()
            .pathParam("storeId", "sga")["/system/store/{storeId}"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .body("$", hasEntry("baz", "quuz"))
    }

    /**
     * Save and load from the store using a key prefix.
     */
    @Test
    fun testSetAndGetByKeyPrefix() {
        // save via system
        RestAssured.given().`when`()
            .pathParam("storeId", "kp")
            .pathParam("key", "foo")
            .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
            .body("bar")
            .put("/system/store/{storeId}/{key}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_CREATED))

        // load all
        RestAssured.given().`when`()
            .pathParam("storeId", "kp")
            .queryParam("keyPrefix", "f")
            .get("/system/store/{storeId}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .body("$", hasEntry("foo", "bar"))
    }

    /**
     * Clear the contents of a store.
     */
    @Test
    fun testClearStore() {
        // save via system
        RestAssured.given().`when`()
            .pathParam("storeId", "dstore")
            .pathParam("key", "baz")
            .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
            .body("quuz")
            .put("/system/store/{storeId}/{key}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_CREATED))

        // delete
        RestAssured.given().`when`()
            .pathParam("storeId", "dstore")
            .delete("/system/store/{storeId}")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT))

        // should not exist
        RestAssured.given().`when`()
            .pathParam("storeId", "dstore")["/system/store/{storeId}"]
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_OK))
            .body("$", not(hasEntry(any(Any::class.java), any(Any::class.java))))
    }
}
