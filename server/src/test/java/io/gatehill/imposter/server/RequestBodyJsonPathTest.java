package io.gatehill.imposter.server;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.test.TestPluginImpl;
import io.gatehill.imposter.server.util.FeatureUtil;
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
 * Tests for matching request body with JsonPath.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class RequestBodyJsonPathTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        // enable store support before boot
        System.setProperty(FeatureUtil.SYS_PROP_IMPOSTER_FEATURES, "stores");

        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/request-body-jsonpath"
        );
    }

    /**
     * Match against a string in the request body
     */
    @Test
    public void testMatchStringInRequestBody() {
        given().when()
                .log().ifValidationFails()
                .body("{ \"foo\": \"bar\" }")
                .get("/example1")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT));
    }

    /**
     * Match against an integer in the request body
     */
    @Test
    public void testMatchIntegerInRequestBody() {
        given().when()
                .log().ifValidationFails()
                .body("{ \"baz\": 99 }")
                .get("/example2")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_MOVED_TEMP));
    }

    /**
     * Match null against an empty JsonPath result in the request body
     */
    @Test
    public void testMatchNullRequestBody() {
        given().when()
                .log().ifValidationFails()
                .body("{ \"foo\": \"bar\" }")
                .get("/example3")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CONFLICT));
    }
}
