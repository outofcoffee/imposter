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
import io.gatehill.imposter.plugin.DynamicPluginDiscoveryStrategyImpl
import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.server.vertxweb.VertxWebServerFactoryImpl
import io.gatehill.imposter.util.MetricsUtil.configureMetrics
import io.vertx.core.VertxOptions
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import java.io.IOException
import java.net.ServerSocket
import java.nio.file.Paths

/**
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner::class)
abstract class BaseVerticleTest {
    @get:Rule
    val rule = RunTestOnContext(configureMetrics(VertxOptions()))

    @Before
    @Throws(Exception::class)
    open fun setUp(testContext: TestContext) {
        val async = testContext.async()

        // simulate ImposterLauncher bootstrap
        ConfigHolder.resetConfig()
        configure(ConfigHolder.config)

        rule.vertx().deployVerticle(ImposterVerticle::class.java.canonicalName) { completion ->
            if (completion.succeeded()) {
                async.complete()
            } else {
                testContext.fail(completion.cause())
            }
        }
    }

    @Throws(Exception::class)
    protected open fun configure(imposterConfig: ImposterConfig) {
        imposterConfig.serverFactory = VertxWebServerFactoryImpl::class.qualifiedName
        imposterConfig.pluginDiscoveryStrategy = DynamicPluginDiscoveryStrategyImpl::class.qualifiedName
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
    }
}