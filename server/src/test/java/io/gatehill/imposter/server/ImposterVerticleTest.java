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

package io.gatehill.imposter.server;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.plugin.test.TestPluginConfig;
import io.gatehill.imposter.plugin.test.TestPluginImpl;
import io.gatehill.imposter.util.CryptoUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.InjectorUtil;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PASSWORD;
import static io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PATH;
import static io.gatehill.imposter.util.FileUtil.CLASSPATH_PREFIX;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner.class)
public class ImposterVerticleTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);

        // set up trust store for TLS
        RestAssured.trustStore(CryptoUtil.getDefaultKeystore(ImposterVerticleTest.class).toFile(), CryptoUtil.DEFAULT_KEYSTORE_PASSWORD);
        RestAssured.baseURI = "https://" + getHost() + ":" + getListenPort();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/simple-config"
        );
    }

    @Override
    protected void configure(ImposterConfig imposterConfig) throws Exception {
        super.configure(imposterConfig);

        // enable TLS
        imposterConfig.setTlsEnabled(true);
        imposterConfig.setKeystorePath(CLASSPATH_PREFIX + DEFAULT_KEYSTORE_PATH);
        imposterConfig.setKeystorePassword(DEFAULT_KEYSTORE_PASSWORD);
    }

    @Test
    public void testPluginLoadAndConfig(TestContext testContext) {
        final PluginManager pluginManager = InjectorUtil.getInjector().getInstance(PluginManager.class);

        final TestPluginImpl plugin = pluginManager.getPlugin(TestPluginImpl.class.getCanonicalName());
        testContext.assertNotNull(plugin);

        testContext.assertNotNull(plugin.getConfigs());
        testContext.assertEquals(1, plugin.getConfigs().size());

        final TestPluginConfig pluginConfig = plugin.getConfigs().get(0);
        testContext.assertEquals("/example", pluginConfig.getPath());
        testContext.assertEquals("test-plugin-data.json", pluginConfig.getResponseConfig().getStaticFile());
        testContext.assertEquals("testValue", pluginConfig.getCustomProperty());
    }

    @Test
    public void testRequestSuccess() throws Exception {
        given().when()
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }
}
