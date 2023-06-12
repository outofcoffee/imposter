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
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Tests for [io.gatehill.imposter.config.util.ConfigUtil].
 *
 * @author Pete Cornish
 */
class ConfigUtilTest {
    @Test
    fun testReadInterpolatedPluginConfig() {
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
     * All config files within the config dir and its subdirectories should be returned.
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
            assertThat("config file should have base path set", config.path, Matchers.startsWith(expectedBasePath))
        }
    }

    private fun buildLoadedConfig(configRef: ConfigReference, configFile: File) =
        LoadedConfig(configRef, configFile.readText(), "io.gatehill.imposter.core.test.ExamplePluginImpl")
}
