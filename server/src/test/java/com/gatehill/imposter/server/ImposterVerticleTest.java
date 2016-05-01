package com.gatehill.imposter.server;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.plugin.test.TestPluginConfig;
import com.gatehill.imposter.plugin.test.TestPluginImpl;
import com.gatehill.imposter.util.CryptoUtil;
import com.gatehill.imposter.util.HttpUtil;
import com.gatehill.imposter.util.InjectorUtil;
import com.jayway.restassured.RestAssured;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PASSWORD;
import static com.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PATH;
import static com.gatehill.imposter.util.FileUtil.CLASSPATH_PREFIX;
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
        super.setUp(testContext);

        // set up trust store for TLS
        RestAssured.trustStore(CryptoUtil.getDefaultKeystore(ImposterVerticleTest.class).toFile(), CryptoUtil.DEFAULT_KEYSTORE_PASSWORD);
        RestAssured.baseURI = "https://" + HOST + ":" + getListenPort();
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
    public void testPluginLoadAndConfig(TestContext testContext) throws Exception {
        final PluginManager pluginManager = InjectorUtil.getInjector().getInstance(PluginManager.class);

        final TestPluginImpl plugin = pluginManager.getPlugin(TestPluginImpl.class.getCanonicalName());
        testContext.assertNotNull(plugin);

        testContext.assertNotNull(plugin.getConfigs());
        testContext.assertEquals(1, plugin.getConfigs().size());

        final TestPluginConfig pluginConfig = plugin.getConfigs().get(0);
        testContext.assertEquals("/example", pluginConfig.getPath());
        testContext.assertEquals("simple-plugin-data.json", pluginConfig.getResponseConfig().getStaticFile());
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
