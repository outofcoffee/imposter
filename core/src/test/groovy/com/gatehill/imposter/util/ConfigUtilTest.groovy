package com.gatehill.imposter.util

import org.junit.Test

import static org.junit.Assert.assertEquals

/**
 * Tests for {@link ConfigUtil}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class ConfigUtilTest {
    @Test
    void testLoadPluginConfigs() {
        def configDir = new File(ConfigUtilTest.class.getResource('/config').toURI()).path

        def configs = ConfigUtil.loadPluginConfigs(configDir)
        assertEquals(1, configs.size())

        def configFiles = configs['com.gatehill.imposter.core.test.ExamplePluginImpl']
        assertEquals(2, configFiles.size())
    }
}
