package io.gatehill.imposter.server;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.server.util.ConfigUtil;
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

import static io.gatehill.imposter.util.HttpUtil.DEFAULT_SERVER_FACTORY;
import static java.util.Collections.emptyMap;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public abstract class BaseVerticleTest {
    protected static final String HOST = "localhost";

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Before
    public void setUp(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        // simulate ImposterLauncher bootstrap
        ConfigUtil.resetConfig();
        configure(ConfigUtil.getConfig());

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
        imposterConfig.setConfigDirs(new String[]{Paths.get(getClass().getResource("/config").toURI()).toString()});
        imposterConfig.setPlugins(new String[]{getPluginClass().getCanonicalName()});
        imposterConfig.setPluginArgs(emptyMap());
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    public int getListenPort() {
        return ConfigUtil.getConfig().getListenPort();
    }

    protected abstract Class<? extends Plugin> getPluginClass();
}
