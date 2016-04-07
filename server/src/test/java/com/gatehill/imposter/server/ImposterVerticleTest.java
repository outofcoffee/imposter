package com.gatehill.imposter.server;

import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.plugin.test.TestPluginConfig;
import com.gatehill.imposter.plugin.test.TestPluginImpl;
import com.gatehill.imposter.util.InjectorUtil;
import com.jayway.restassured.RestAssured;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.nio.file.Paths;

import static com.gatehill.imposter.server.ImposterVerticle.CONFIG_PREFIX;
import static com.jayway.restassured.RestAssured.given;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class ImposterVerticleTest {
    private static final int LISTEN_PORT = 8443;
    private static final String HOST = "localhost";

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Before
    public void setUp(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        System.setProperty(CONFIG_PREFIX + "configDir", Paths.get(ImposterVerticleTest.class.getResource("/config").toURI()).toString());
        System.setProperty(CONFIG_PREFIX + "pluginClass", TestPluginImpl.class.getCanonicalName());
        System.setProperty(CONFIG_PREFIX + "host", HOST);
        System.setProperty(CONFIG_PREFIX + "listenPort", String.valueOf(LISTEN_PORT));

        rule.vertx().deployVerticle(ImposterVerticle.class.getCanonicalName(), completion -> {
            if (completion.succeeded()) {
                async.complete();
            } else {
                testContext.fail(completion.cause());
            }
        });

        RestAssured.baseURI = "http://" + HOST + ":" + LISTEN_PORT;
    }

    @Test
    public void testPluginLoadAndConfig(TestContext testContext) throws Exception {
        final PluginManager pluginManager = InjectorUtil.getInjector().getInstance(PluginManager.class);

        final TestPluginImpl plugin = pluginManager.getPlugin(TestPluginImpl.class.getCanonicalName());
        testContext.assertNotNull(plugin);

        testContext.assertNotNull(plugin.getConfigs());
        testContext.assertEquals(1, plugin.getConfigs().size());

        final TestPluginConfig pluginConfig = plugin.getConfigs().get(0);
        testContext.assertEquals("/example", pluginConfig.getBaseUrl());
        testContext.assertEquals("simple-plugin-data.json.json", pluginConfig.getResponseFile());
        testContext.assertEquals("testValue", pluginConfig.getCustomProperty());
    }

    @Test
    public void testRequestSuccess(TestContext testContext) throws Exception {
        given().when()
                .get("/example")
                .then()
                .statusCode(HttpURLConnection.HTTP_OK);
    }
}
