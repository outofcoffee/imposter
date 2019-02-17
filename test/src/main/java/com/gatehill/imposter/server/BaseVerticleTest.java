package com.gatehill.imposter.server;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.util.InjectorUtil;
import com.google.common.collect.Lists;
import com.google.inject.Module;
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

import static com.gatehill.imposter.util.HttpUtil.DEFAULT_SERVER_FACTORY;
import static java.util.Collections.emptyMap;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public abstract class BaseVerticleTest {
    protected static final String HOST = "localhost";

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    private int listenPort;

    public int getListenPort() {
        return listenPort;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        listenPort = findFreePort();

        // simulate ImposterLauncher injector bootstrap
        final Module[] modules = Lists.asList(new BootstrapModule(DEFAULT_SERVER_FACTORY), getAdditionalModules()).toArray(new Module[0]);

        final ImposterConfig imposterConfig = InjectorUtil.create(modules).getInstance(ImposterConfig.class);
        configure(imposterConfig);

        rule.vertx().deployVerticle(ImposterVerticle.class.getCanonicalName(), completion -> {
            if (completion.succeeded()) {
                async.complete();
            } else {
                testContext.fail(completion.cause());
            }
        });
    }

    protected Module[] getAdditionalModules() {
        return new Module[0];
    }

    protected void configure(ImposterConfig imposterConfig) throws Exception {
        imposterConfig.setConfigDirs(new String[]{Paths.get(getClass().getResource("/config").toURI()).toString()});
        imposterConfig.setPluginClassNames(new String[]{getPluginClass().getCanonicalName()});
        imposterConfig.setHost(HOST);
        imposterConfig.setListenPort(listenPort);
        imposterConfig.setPluginArgs(emptyMap());
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    protected abstract Class<? extends Plugin> getPluginClass();
}
