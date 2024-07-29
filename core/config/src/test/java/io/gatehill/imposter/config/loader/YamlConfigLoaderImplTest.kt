package io.gatehill.imposter.config.loader

import io.gatehill.imposter.config.ConfigHolder
import io.gatehill.imposter.config.ConfigReference
import io.gatehill.imposter.config.ConfigUtilTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Tests for [YamlConfigLoaderImpl].
 *
 * @author Pete Cornish
 */
class YamlConfigLoaderImplTest {
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

        val loader = YamlConfigLoaderImpl()
        loader.initInterpolators(environment)

        val configFile = File(ConfigUtilTest::class.java.getResource("/interpolated/test-config.yaml").toURI())
        val configRef = ConfigReference(
            file = configFile,
            configRoot = configFile.parentFile,
        )
        val loadedConfig = loader.readPluginConfig(configRef)
        assertEquals("example-plugin", loadedConfig.plugin)
        MatcherAssert.assertThat(loadedConfig.serialised, Matchers.containsString("port 9090"))
    }

    @Test
    fun readPluginConfig() {
    }

    @Test
    fun loadConfig() {
    }
}
