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
package io.gatehill.imposter.config

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.support.BasePathSupportingPluginConfig
import io.gatehill.imposter.config.util.ConfigUtil
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.http.HttpMethod
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.nullValue
import org.hamcrest.Matchers.startsWith
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tests for [io.gatehill.imposter.config.util.ConfigUtil].
 *
 * @author Pete Cornish
 */
class ConfigUtilTest {
    companion object {
        @AfterClass
        @JvmStatic
        fun afterClass() {
            ConfigHolder.config.listenPort = 0
        }
    }

    @Test
    fun testReadInterpolatedPluginConfig() {
        ConfigHolder.config.listenPort = 9090

        // override environment variables in string interpolators
        val environment: Map<String, String> = mapOf(
            "EXAMPLE_PLUGIN" to "example-plugin"
        )
        ConfigUtil.initInterpolators(environment)

        val configFile = File(ConfigUtilTest::class.java.getResource("/interpolated/test-config.yaml").toURI())
        val configRef = ConfigReference(
            file = configFile,
            configRoot = configFile.parentFile,
        )
        val loadedConfig = ConfigUtil.readPluginConfig(configRef)
        assertEquals("example-plugin", loadedConfig.plugin)
        assertThat(loadedConfig.serialised, containsString("port 9090"))
    }

    /**
     * All config files within the config dir and its subdirectories should be returned.
     */
    @Test
    fun testLoadRecursive_Enabled() {
        val configDir = File(ConfigUtilTest::class.java.getResource("/recursive").toURI())
        val configFiles = ConfigUtil.listConfigFiles(configDir, true, emptyList())

        assertEquals(3, configFiles.size)
        assertTrue(
            "discovered files should include top level dir config",
            configFiles.map { it.file.toString() }.any { it.endsWith("/recursive/test-config.yaml") }
        )
        assertTrue(
            "discovered files should include subdir1 config",
            configFiles.map { it.file.toString() }.any { it.endsWith("/recursive/subdir1/test-config.yaml") }
        )
        assertTrue(
            "discovered files should include subdir2 config",
            configFiles.map { it.file.toString() }.any { it.endsWith("/recursive/subdir2/test-config.yaml") }
        )
    }

    /**
     * A subset of the config files within the directory will be returned, subject to the
     * exclusion list passed.
     */
    @Test
    fun testLoadRecursive_WithExclusions() {
        val configDir = File(ConfigUtilTest::class.java.getResource("/recursive").toURI())
        val configFiles = ConfigUtil.listConfigFiles(configDir, true, listOf("subdir2"))

        assertEquals(2, configFiles.size)
        assertTrue(
            "discovered files should include top level dir config",
            configFiles.map { it.file.toString() }.any { it.endsWith("/recursive/test-config.yaml") }
        )
        assertTrue(
            "discovered files should include subdir1 config",
            configFiles.map { it.file.toString() }.any { it.endsWith("/recursive/subdir1/test-config.yaml") }
        )
    }

    /**
     * Only the top level config file within the config dir and its subdirectories should be returned.
     */
    @Test
    fun testLoadRecursive_Disabled() {
        val configDir = File(ConfigUtilTest::class.java.getResource("/recursive").toURI())
        val configFiles = ConfigUtil.listConfigFiles(configDir, false, emptyList())

        assertEquals(1, configFiles.size)
        assertTrue(
            "discovered files should include top level dir config",
            configFiles.map { it.file.toString() }.any { it.endsWith("/recursive/test-config.yaml") }
        )
    }

    /**
     * The configuration should be parsed correctly.
     */
    @Test
    fun testLoadConfig() {
        EnvVars.populate("IMPOSTER_AUTO_BASE_PATH" to "false")
        val configFile = File(ConfigUtilTest::class.java.getResource("/simple/test-config.yaml").toURI())
        val configRef = ConfigReference(
            file = configFile,
            configRoot = configFile.parentFile,
        )
        val loadedConfig = ConfigUtil.readPluginConfig(configRef)
        val config = ConfigUtil.loadPluginConfig(
            ImposterConfig(),
            loadedConfig,
            BasePathSupportingPluginConfig::class.java,
        )
        assertThat("plugin should be set", config.plugin, not(nullValue()))
        assertThat("empty root path should be null", config.path, nullValue())
        assertThat("empty root response config should be empty", config.responseConfig.hasConfiguration(), equalTo(false))

        val exampleResource = config.resources?.find { it.path == "/example" }
        assertNotNull("example resource should be set", exampleResource)
        assertThat("resource path should be set", exampleResource?.path, equalTo("/example"))
        assertThat("resource method should be set", exampleResource?.method, equalTo(HttpMethod.GET))
        assertThat("resource response config should not be empty", exampleResource?.responseConfig?.hasConfiguration(), equalTo(true))
        assertThat("resource response status code should be set", exampleResource?.responseConfig?.statusCode, equalTo(200))
        assertThat("resource response content should be set", exampleResource?.responseConfig?.content, equalTo("example"))

        val openApiStyleResource = config.resources?.find { it.path?.startsWith("/openapi-style") == true }
        assertNotNull("openapi style resource should be set", openApiStyleResource)
        assertThat("openapi style resource path should be converted to vertx style", openApiStyleResource?.path, equalTo("/openapi-style/:param1"))
    }

    /**
     * The base path should be applied to the resource path, but not the empty root path.
     */
    @Test
    fun testApplyBasePath() {
        EnvVars.populate("IMPOSTER_AUTO_BASE_PATH" to "false")
        val configFile = File(ConfigUtilTest::class.java.getResource("/basepath/test-config.yaml").toURI())
        val configRef = ConfigReference(
            file = configFile,
            configRoot = configFile.parentFile,
        )
        val loadedConfig = ConfigUtil.readPluginConfig(configRef)
        val config = ConfigUtil.loadPluginConfig(
            ImposterConfig(),
            loadedConfig,
            BasePathSupportingPluginConfig::class.java,
        )
        assertThat("empty root path should be prefixed with base path", config.path, not(startsWith("/base/")))
        assertThat("resource path should be prefixed with base path", config.resources?.first()?.path, startsWith("/base/"))
    }

    /**
     * The `basePath` should be set on the relative directory from the config root.
     */
    @Test
    fun testAutoBasePath() {
        EnvVars.populate("IMPOSTER_AUTO_BASE_PATH" to "true")
        val configDir = File(ConfigUtilTest::class.java.getResource("/recursive").toURI())
        val configFiles = ConfigUtil.listConfigFiles(configDir, true, emptyList())

        assertEquals(3, configFiles.size)

        for (configFile in configFiles) {
            val loadedConfig = buildLoadedConfig(configFile, configFile.file)
            val config = ConfigUtil.loadPluginConfig(
                ImposterConfig(),
                loadedConfig,
                BasePathSupportingPluginConfig::class.java,
            )

            val expectedBasePath = configFile.file.canonicalPath.substring(configFile.configRoot.canonicalPath.length).substringBeforeLast(File.separator)
            assertThat("config file should have base path set", config.path, startsWith(expectedBasePath))
        }
    }

    private fun buildLoadedConfig(configRef: ConfigReference, configFile: File) =
        LoadedConfig(configRef, configFile.readText(), "io.gatehill.imposter.core.test.ExamplePluginImpl")
}
