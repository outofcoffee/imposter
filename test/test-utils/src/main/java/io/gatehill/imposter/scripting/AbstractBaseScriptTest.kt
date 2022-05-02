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

package io.gatehill.imposter.scripting

import com.google.inject.Guice
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import io.gatehill.imposter.script.RuntimeContext
import io.gatehill.imposter.script.ScriptUtil
import io.gatehill.imposter.service.ScriptService
import io.gatehill.imposter.util.FeatureUtil
import io.gatehill.imposter.util.MetricsUtil
import org.apache.logging.log4j.LogManager
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.mockito.Mockito.mock
import java.nio.file.Paths
import org.mockito.Mockito.`when` as When

/**
 * @author Pete Cornish
 */
abstract class AbstractBaseScriptTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            FeatureUtil.disableFeature(MetricsUtil.FEATURE_NAME_METRICS)
        }

        @JvmStatic
        @AfterClass
        fun afterClass() {
            FeatureUtil.clearSystemPropertyOverrides()
        }
    }

    @Before
    fun setUp() {
        Guice.createInjector().injectMembers(this)
    }

    protected abstract fun getService(): ScriptService

    protected abstract fun getScriptName(): String

    protected fun configureScript(): PluginConfig {
        val script =
            Paths.get(AbstractBaseScriptTest::class.java.getResource("/script/${getScriptName()}").toURI())

        return PluginConfigImpl().apply {
            parentDir = script.parent.toFile()
            responseConfig.apply {
                this.scriptFile = getScriptName()
            }
        }
    }

    protected fun buildRuntimeContext(
        additionalBindings: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        pathParams: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
        env: Map<String, String> = emptyMap(),
        body: String = ""
    ): RuntimeContext {
        val logger = LogManager.getLogger("script-engine-test")

        val mockRequest = mock(HttpRequest::class.java)
        When(mockRequest.method()).thenReturn(HttpMethod.GET)
        When(mockRequest.path()).thenReturn("/example")
        When(mockRequest.absoluteURI()).thenReturn("http://localhost:8080/example")
        When(mockRequest.headers()).thenReturn(headers)

        val mockHttpExchange = mock(HttpExchange::class.java)
        When(mockHttpExchange.request()).thenReturn(mockRequest)
        When(mockHttpExchange.pathParams()).thenReturn(pathParams)
        When(mockHttpExchange.queryParams()).thenReturn(queryParams)
        When(mockHttpExchange.bodyAsString).thenReturn(body)

        val pluginConfig = mock(PluginConfig::class.java)
        val executionContext = ScriptUtil.buildContext(mockHttpExchange, null)
        return RuntimeContext(env, logger, pluginConfig, additionalBindings, executionContext)
    }
}
