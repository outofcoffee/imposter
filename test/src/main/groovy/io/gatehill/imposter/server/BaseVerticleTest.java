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
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.util.MetricsUtil;
import io.vertx.core.VertxOptions;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Paths;
import java.util.List;

import static io.gatehill.imposter.util.HttpUtil.DEFAULT_SERVER_FACTORY;
import static java.util.Collections.emptyMap;

/**
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner.class)
public abstract class BaseVerticleTest {
    protected static final String HOST = "localhost";

    @Rule
    public RunTestOnContext rule = new RunTestOnContext(MetricsUtil.configureMetrics(new VertxOptions()));

    @Before
    public void setUp(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        // simulate ImposterLauncher bootstrap
        ConfigHolder.resetConfig();
        configure(ConfigHolder.getConfig());

        rule.vertx().deployVerticle(ImposterVerticle.class.getCanonicalName(), completion -> {
            if (completion.succeeded()) {
                async.complete();
            } else {
                testContext.fail(completion.cause());
            }
        });
    }

    protected void configure(ImposterConfig imposterConfig) throws Exception {
        imposterConfig.setServerFactory(DEFAULT_SERVER_FACTORY);
        imposterConfig.setHost(HOST);
        imposterConfig.setListenPort(findFreePort());
        imposterConfig.setPlugins(new String[]{getPluginClass().getCanonicalName()});
        imposterConfig.setPluginArgs(emptyMap());

        imposterConfig.setConfigDirs(getTestConfigDirs().stream().map(dir -> {
            try {
                return Paths.get(getClass().getResource(dir).toURI()).toString();
            } catch (Exception e) {
                throw new RuntimeException("Error parsing directory: " + dir, e);
            }
        }).toArray(String[]::new));
    }

    /**
     * @return the relative path under the test resources directory, starting with a slash, e.g "/my-config"
     */
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/config"
        );
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public int getListenPort() {
        return ConfigHolder.getConfig().getListenPort();
    }

    protected abstract Class<? extends Plugin> getPluginClass();
}
