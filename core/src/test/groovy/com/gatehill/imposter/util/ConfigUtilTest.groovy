package com.gatehill.imposter.util

import com.gatehill.imposter.plugin.PluginManager
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

        def configFiles = configs['com.gatehill.imposter.core.test.ExamplePluginImpl']
        assertEquals(2, configFiles.size())
    }
}
