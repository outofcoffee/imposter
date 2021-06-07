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
 * Tests for security using query parameters.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class SecurityQueryParamsTest extends BaseVerticleTest {
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
                "/security-config-query-params"
        );
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
                .queryParam("apiKey", "invalid-value")
                .queryParam("userKey", "opensesame")
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
                .queryParam("apiKey", "s3cr3t")
                .queryParam("userKey", "does-not-match")
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
                .queryParam("apiKey", "s3cr3t")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));

        given().when()
                .queryParam("userKey", "opensesame")
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
                .queryParam("apiKey", "s3cr3t")
                .queryParam("userKey", "opensesame")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }
}
