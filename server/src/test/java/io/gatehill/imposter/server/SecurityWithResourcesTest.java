package io.gatehill.imposter.server;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.test.TestPluginImpl;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for when resources are configured with no security, so policy
 * should fall back to root security.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class SecurityWithResourcesTest extends BaseVerticleTest {
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
                "/security-config-with-resources"
        );
    }

    /**
     * Deny - no authentication provided.
     * Resource has no security: should fall back to root.
     */
    @Test
    public void testRequestDenied_NoAuth() {
        given().when()
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));
    }

    /**
     * Permit - both conditions are satisfied.
     * Resource has no security: should fall back to root.
     */
    @Test
    public void testRequestPermitted() {
        given().when()
                .queryParam("apiKey", "s3cr3t")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }
}
