package com.gatehill.imposter.server;

import com.gatehill.imposter.plugin.Plugin;
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

import static com.gatehill.imposter.Imposter.CONFIG_PREFIX;

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

        System.setProperty(CONFIG_PREFIX + "configDir", Paths.get(getClass().getResource("/config").toURI()).toString());
        System.setProperty(CONFIG_PREFIX + "plugin", getPluginClass().getCanonicalName());
        System.setProperty(CONFIG_PREFIX + "host", HOST);
        System.setProperty(CONFIG_PREFIX + "listenPort", String.valueOf(listenPort));

        rule.vertx().deployVerticle(ImposterVerticle.class.getCanonicalName(), completion -> {
            if (completion.succeeded()) {
                async.complete();
            } else {
                testContext.fail(completion.cause());
            }
        });
    }

    private int findFreePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    protected abstract Class<? extends Plugin> getPluginClass();
}
