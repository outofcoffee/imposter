package com.gatehill.imposter.server;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.util.InjectorUtil;
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
        final ImposterConfig imposterConfig = InjectorUtil.create(new BootstrapModule()).getInstance(ImposterConfig.class);
        configure(imposterConfig);

        rule.vertx().deployVerticle(ImposterVerticle.class.getCanonicalName(), completion -> {
            if (completion.succeeded()) {
                async.complete();
            } else {
                testContext.fail(completion.cause());
            }
        });
    }

    protected void configure(ImposterConfig imposterConfig) throws Exception {
        imposterConfig.setConfigDirs(new String[]{Paths.get(getClass().getResource("/config").toURI()).toString()});
        imposterConfig.setPluginClassNames(new String[]{getPluginClass().getCanonicalName()});
        imposterConfig.setHost(HOST);
        imposterConfig.setListenPort(listenPort);
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    protected abstract Class<? extends Plugin> getPluginClass();
}
