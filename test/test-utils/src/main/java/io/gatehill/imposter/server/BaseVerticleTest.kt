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

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.ConfigHolder
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.plugin.DynamicPluginDiscoveryStrategyImpl
import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.server.engine.GoMockEngine
import io.gatehill.imposter.server.engine.JvmMockEngine
import io.gatehill.imposter.server.engine.TestMockEngine
import io.gatehill.imposter.server.vertxweb.VertxWebServerFactoryImpl
import io.vertx.core.Vertx
import io.vertx.junit5.VertxExtension
import io.vertx.junit5.VertxTestContext
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.net.ServerSocket
import java.nio.file.Paths

/**
 * @author Pete Cornish
 */
@ExtendWith(VertxExtension::class)
@VerticleTest
abstract class BaseVerticleTest {
    private val logger = LogManager.getLogger(BaseVerticleTest::class.java)

    private var testEngine: TestMockEngine? = null

    @BeforeEach
    @Throws(Exception::class)
    open fun setUp(vertx: Vertx, testContext: VertxTestContext) {
        ConfigHolder.resetConfig()
        configure(ConfigHolder.config)

        logger.info("Using test engine: $testEngineType")
        testEngine = when (testEngineType) {
            TestEngine.JVM -> JvmMockEngine()
            TestEngine.GO -> GoMockEngine()
        }.also { engine ->
            engine.start(vertx, host, testContext)
        }
    }

    @AfterEach
    fun tearDown() {
        try {
            testEngine?.stop()
        } finally {
            testEngine = null
        }
    }

    @Throws(Exception::class)
    protected open fun configure(imposterConfig: ImposterConfig) {
        imposterConfig.serverFactory = VertxWebServerFactoryImpl::class.qualifiedName
        imposterConfig.pluginDiscoveryStrategyClass = DynamicPluginDiscoveryStrategyImpl::class.qualifiedName
        imposterConfig.host = host
        imposterConfig.listenPort = findFreePort()
        imposterConfig.plugins = arrayOf(pluginClass.canonicalName)
        imposterConfig.pluginArgs = emptyMap()
        imposterConfig.configDirs = testConfigDirs.map { dir: String ->
            try {
                return@map Paths.get(javaClass.getResource(dir).toURI()).toString()
            } catch (e: Exception) {
                throw RuntimeException("Error parsing directory: $dir", e)
            }
        }.toTypedArray()
    }

    /**
     * @return the relative path under the test resources directory, starting with a slash, e.g "/my-config"
     */
    protected open val testConfigDirs = listOf("/config")

    @Throws(IOException::class)
    private fun findFreePort() = ServerSocket(0).use { it.localPort }

    val listenPort: Int
        get() = ConfigHolder.config.listenPort

    protected abstract val pluginClass: Class<out Plugin?>

    companion object {
        @JvmStatic
        protected val host = "localhost"

        @JvmStatic
        @BeforeAll
        fun beforeClass() {
            EnvVars.reset(emptyList())
        }
    }

    enum class TestEngine {
        JVM, GO
    }

    private val testEngineType: TestEngine
        get() = if (System.getenv("TEST_ENGINE") == "go") TestEngine.GO else TestEngine.JVM
}
