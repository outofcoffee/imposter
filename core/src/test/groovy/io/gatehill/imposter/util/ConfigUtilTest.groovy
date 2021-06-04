package io.gatehill.imposter.util


import io.gatehill.imposter.plugin.PluginManager
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 * Tests for {@link ConfigUtil}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class ConfigUtilTest {
    PluginManager pluginManager

    @Before
    void setUp() throws Exception {
        pluginManager = new PluginManager()
    }

    @Test
    void testLoadPluginConfigs() {
        def configDir = new File(ConfigUtilTest.class.getResource('/config').toURI()).path

        def configs = ConfigUtil.loadPluginConfigs(pluginManager, [configDir] as String[])
        assertEquals(1, configs.size())

        def configFiles = configs['io.gatehill.imposter.core.test.ExamplePluginImpl']
        assertEquals(2, configFiles.size())
    }

    @Test
    void testLoadLegacyPluginConfig() {
        def configDir = new File(ConfigUtilTest.class.getResource('/legacy').toURI()).path

        def configs = ConfigUtil.loadPluginConfigs(pluginManager, [configDir] as String[])
        assertEquals(1, configs.size())

        def configFiles = configs['io.gatehill.imposter.core.test.ExamplePluginImpl']
        assertEquals(1, configFiles.size())
    }

    @Test
    void testLoadInterpolatedPluginConfig() {
        // override environment variables in string interpolators
        def environment = ["EXAMPLE_PATH" : "/test"]
        ConfigUtil.initInterpolators(environment)

        def configFile = new File(ConfigUtilTest.class.getResource('/interpolated/test-config.yaml').toURI())

        def config = ConfigUtil.loadPluginConfig(configFile, PluginConfigImpl.class)

        assertEquals("/test", config.path)
    }
}
