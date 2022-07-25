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

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.plugin.test.TestPluginImpl
import io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PASSWORD
import io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PATH
import io.gatehill.imposter.util.CryptoUtil.getDefaultKeystore
import io.gatehill.imposter.util.FileUtil.CLASSPATH_PREFIX
import io.gatehill.imposter.util.HttpUtil
import io.gatehill.imposter.util.InjectorUtil.injector
import io.restassured.RestAssured
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
class ImposterVerticleTest : BaseVerticleTest() {
    override val pluginClass = TestPluginImpl::class.java

    @Before
    @Throws(Exception::class)
    override fun setUp(testContext: TestContext) {
        super.setUp(testContext)

        // set up trust store for TLS
        RestAssured.trustStore(getDefaultKeystore(ImposterVerticleTest::class.java).toFile(), DEFAULT_KEYSTORE_PASSWORD)
        RestAssured.baseURI = "https://$host:$listenPort"
    }

    override val testConfigDirs = listOf(
        "/simple-config"
    )

    @Throws(Exception::class)
    override fun configure(imposterConfig: ImposterConfig) {
        super.configure(imposterConfig)

        // enable TLS
        imposterConfig.isTlsEnabled = true
        imposterConfig.keystorePath = CLASSPATH_PREFIX + DEFAULT_KEYSTORE_PATH
        imposterConfig.keystorePassword = DEFAULT_KEYSTORE_PASSWORD
    }

    @Test
    fun testPluginLoadAndConfig(testContext: TestContext) {
        val pluginManager = injector!!.getInstance(PluginManager::class.java)
        val plugin = pluginManager.getPlugin<TestPluginImpl>(TestPluginImpl::class.java.canonicalName)
        testContext.assertNotNull(plugin)
        testContext.assertNotNull(plugin!!.configs)
        testContext.assertEquals(1, plugin.configs.size)

        val pluginConfig = plugin.configs[0]
        testContext.assertEquals("/example", pluginConfig.path)
        testContext.assertEquals("test-plugin-data.json", pluginConfig.responseConfig.staticFile)
        testContext.assertEquals("testValue", pluginConfig.customProperty)
    }

    @Test
    fun testRequestSuccess() {
        RestAssured.given().`when`()
            .get("/example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_OK))
    }

    @Test
    fun testRequestNotFound() {
        RestAssured.given().`when`()
            .get("/does_not_match")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_NOT_FOUND))
    }

    @Test
    fun testResponseFileNotFound() {
        RestAssured.given().`when`()
            .get("/static-file-example")
            .then()
            .statusCode(Matchers.equalTo(HttpUtil.HTTP_NOT_FOUND))
    }
}
