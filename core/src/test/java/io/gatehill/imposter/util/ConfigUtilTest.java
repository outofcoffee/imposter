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

package io.gatehill.imposter.util;

import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.plugin.config.PluginConfigImpl;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ConfigUtil}.
 *
 * @author Pete Cornish
 */
public class ConfigUtilTest {
    PluginManager pluginManager;

    @Before
    public void setUp() throws Exception {
        pluginManager = new PluginManager();
    }

    @Test
    public void testLoadPluginConfigs() throws Exception {
        String configDir = new File(ConfigUtilTest.class.getResource("/config").toURI()).getPath();

        Map<String, List<File>> configs = ConfigUtil.loadPluginConfigs(pluginManager, new String[]{configDir});
        assertEquals(1, configs.size());

        List<File> configFiles = configs.get("io.gatehill.imposter.core.test.ExamplePluginImpl");
        assertEquals(2, configFiles.size());
    }

    @Test
    public void testLoadLegacyPluginConfig() throws Exception {
        String configDir = new File(ConfigUtilTest.class.getResource("/legacy").toURI()).getPath();

        Map<String, List<File>> configs = ConfigUtil.loadPluginConfigs(pluginManager, new String[]{configDir});
        assertEquals(1, configs.size());

        List<File> configFiles = configs.get("io.gatehill.imposter.core.test.ExamplePluginImpl");
        assertEquals(1, configFiles.size());
    }

    @Test
    public void testLoadInterpolatedPluginConfig() throws Exception {
        // override environment variables in string interpolators
        Map<String, String> environment = new HashMap<String, String>() {{
            put("EXAMPLE_PATH", "/test");
        }};
        ConfigUtil.initInterpolators(environment);

        File configFile = new File(ConfigUtilTest.class.getResource("/interpolated/test-config.yaml").toURI());

        PluginConfigImpl config = ConfigUtil.loadPluginConfig(configFile, PluginConfigImpl.class, true, true);

        assertEquals("/test", config.getPath());
    }
}
