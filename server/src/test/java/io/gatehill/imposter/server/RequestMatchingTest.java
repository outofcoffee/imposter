package io.gatehill.imposter.server;

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

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for matching path parameters.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class RequestMatchingTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return newArrayList(
                "/request-matching"
        );
    }

    /**
     * Match against a path parameter defined in configuration in Vert.x format.
     */
    @Test
    public void testMatchPathParamVertxFormat() {
        given().when()
                .get("/users/1")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT));
    }

    /**
     * Match against a path parameter defined in configuration in OpenAPI format.
     */
    @Test
    public void testMatchPathParamOpenApiFormat() {
        given().when()
                .get("/orders/99")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NOT_AUTHORITATIVE));
    }
}
