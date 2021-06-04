package io.gatehill.imposter.server;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.plugin.config.security.HttpHeader;
import io.gatehill.imposter.plugin.config.security.SecurityCondition;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.gatehill.imposter.plugin.config.security.SecurityEffect;
import io.gatehill.imposter.plugin.test.TestPluginConfig;
import io.gatehill.imposter.plugin.test.TestPluginImpl;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.InjectorUtil;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class SecurityConfigTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/security-config"
        );
    }

    @Test
    public void testPluginLoadAndConfig(TestContext testContext) {
        final PluginManager pluginManager = InjectorUtil.getInjector().getInstance(PluginManager.class);

        final TestPluginImpl plugin = pluginManager.getPlugin(TestPluginImpl.class.getCanonicalName());
        testContext.assertNotNull(plugin);

        testContext.assertNotNull(plugin.getConfigs());
        testContext.assertEquals(1, plugin.getConfigs().size());

        final TestPluginConfig pluginConfig = plugin.getConfigs().get(0);

        // check security config
        final SecurityConfig securityConfig = pluginConfig.getSecurity();
        testContext.assertNotNull(securityConfig);
        testContext.assertEquals(SecurityEffect.Deny, securityConfig.getDefaultEffect());

        // check conditions
        testContext.assertEquals(2, securityConfig.getConditions().size());

        // check short configuration option
        final SecurityCondition condition1 = securityConfig.getConditions().get(0);
        testContext.assertEquals(SecurityEffect.Permit, condition1.getEffect());
        final Map<String, HttpHeader> parsedHeaders1 = condition1.getParsedHeaders();
        testContext.assertEquals(1, parsedHeaders1.size());
        testContext.assertEquals("s3cr3t", parsedHeaders1.get("Authorization").getValue());

        // check long configuration option
        final SecurityCondition condition2 = securityConfig.getConditions().get(1);
        testContext.assertEquals(SecurityEffect.Deny, condition2.getEffect());
        final Map<String, HttpHeader> parsedHeaders2 = condition2.getParsedHeaders();
        testContext.assertEquals(1, parsedHeaders2.size());
        testContext.assertEquals("opensesame", parsedHeaders2.get("X-Api-Key").getValue());
    }

    /**
     * Deny - no authentication provided.
     */
    @Test
    public void testRequestDenied_NoAuth() {
        given().when()
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));
    }

    /**
     * Deny - the 'Permit' condition does not match.
     */
    @Test
    public void testRequestDenied_NoPermitMatch() {
        given().when()
                .header("Authorization", "invalid-value")
                .header("X-Api-Key", "opensesame")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));
    }

    /**
     * Deny - the 'Deny' condition matches.
     */
    @Test
    public void testRequestDenied_DenyMatch() {
        given().when()
                .header("Authorization", "s3cr3t")
                .header("X-Api-Key", "does-not-match")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));
    }

    /**
     * Deny - only one condition satisfied.
     */
    @Test
    public void testRequestDenied_OnlyOneMatch() {
        given().when()
                .header("Authorization", "s3cr3t")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));

        given().when()
                .header("X-Api-Key", "opensesame")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));
    }

    /**
     * Permit - both conditions are satisfied.
     */
    @Test
    public void testRequestPermitted() {
        given().when()
                .header("Authorization", "s3cr3t")
                .header("X-Api-Key", "opensesame")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }
}
