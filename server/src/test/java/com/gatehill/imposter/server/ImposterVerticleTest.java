package com.gatehill.imposter.server;

import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.plugin.test.TestPluginConfig;
import com.gatehill.imposter.plugin.test.TestPluginImpl;
import com.gatehill.imposter.util.CryptoUtil;
import com.gatehill.imposter.util.InjectorUtil;
import com.jayway.restassured.RestAssured;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;

import static com.gatehill.imposter.Imposter.CONFIG_PREFIX;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class ImposterVerticleTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        // enable TLS before deployment
        System.setProperty(CONFIG_PREFIX + "tls", "true");

        // deploy
        super.setUp(testContext);

        // set up trust store for TLS
        RestAssured.trustStore(CryptoUtil.getDefaultKeystore(ImposterVerticleTest.class).toFile(), CryptoUtil.DEFAULT_KEYSTORE_PASSWORD);
        RestAssured.baseURI = "https://" + HOST + ":" + getListenPort();
    }

    @Test
    public void testPluginLoadAndConfig(TestContext testContext) throws Exception {
        final PluginManager pluginManager = InjectorUtil.getInjector().getInstance(PluginManager.class);

        final TestPluginImpl plugin = pluginManager.getPlugin(TestPluginImpl.class.getCanonicalName());
        testContext.assertNotNull(plugin);

        testContext.assertNotNull(plugin.getConfigs());
        testContext.assertEquals(1, plugin.getConfigs().size());

        final TestPluginConfig pluginConfig = plugin.getConfigs().get(0);
        testContext.assertEquals("/example", pluginConfig.getBasePath());
        testContext.assertEquals("simple-plugin-data.json", pluginConfig.getResponseFile());
        testContext.assertEquals("testValue", pluginConfig.getCustomProperty());
    }

    @Test
    public void testRequestSuccess() throws Exception {
        given().when()
                .get("/example")
                .then()
                .statusCode(equalTo(HttpURLConnection.HTTP_OK));
    }
}
