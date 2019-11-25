package io.gatehill.imposter.util


import io.gatehill.imposter.plugin.PluginManager
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
}
