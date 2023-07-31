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

import io.gatehill.imposter.plugin.test.TestPluginImpl
import io.gatehill.imposter.util.HttpUtil
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.equalTo
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer

/**
 * Tests for remote steps.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class StepsRemoteTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$host:$listenPort"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    override val testConfigDirs = listOf(
        "/steps-remote"
    )

    /**
     * Execute a remote step.
     */
    @Test
    fun `execute remote step`() {
        val httpServer = vertx!!.createHttpServer(HttpServerOptions().setPort(8081))
        httpServer.requestHandler { request ->
            request.failOnAssertionError(request.method(), equalTo(HttpMethod.POST))
            request.failOnAssertionError(request.path(), equalTo("/"))
            request.failOnAssertionError(request.getHeader("X-Test-Header"), equalTo("test"))
            request.body { body ->
                request.failOnAssertionError(body.result().toString(), equalTo("petId=123"))
                request.response().end("Fluffy")
            }
        }
        blockWait { listenHandler -> httpServer.listen(listenHandler) }

        given().`when`()
            .queryParam("petId", "123")
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
            .body(equalTo("Fluffy"))
    }

    private fun <T> HttpServerRequest.failOnAssertionError(
        actual: T,
        assertion: Matcher<in T>,
    ) {
        try {
            assertThat(actual, assertion)
        } catch (e: AssertionError) {
            response().setStatusCode(400).end(e.message)
            throw e
        }
    }

    companion object {
        private var vertx: Vertx? = null

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            vertx = Vertx.vertx()
        }

        @JvmStatic
        @AfterClass
        @Throws(Exception::class)
        fun afterClass() {
            vertx?.close()
        }

        /**
         * Block the consumer until the handler is called.
         *
         * @param handlerConsumer the consumer of the handler
         * @param <T>             the type of the async result
         */
        @Throws(Exception::class)
        private fun <T> blockWait(handlerConsumer: Consumer<Handler<T>>) {
            val latch = CountDownLatch(1)
            val handler = Handler { _: T -> latch.countDown() }
            handlerConsumer.accept(handler)
            latch.await()
        }
    }
}
