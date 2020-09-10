package io.gatehill.imposter.plugin.openapi;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for scripted responses.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ScriptedResponseTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return OpenApiPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/openapi2/scripted"
        );
    }

    /**
     * Should return the example from the specification when the script triggers an HTTP 201 Created status code.
     */
    @Test
    public void testServeScriptedExample() {
        given()
                .log().ifValidationFails()
                // JSON content type in 'Accept' header matches specification example
                .accept(ContentType.JSON)
                .when()
                .put("/simple/apis")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_CREATED)
                .body("result", equalTo("success"))
                .header("MyHeader", "MyHeaderValue");
    }

    @Test
    public void testRequestWithHeaders() {
        given()
                .log().ifValidationFails()
                .accept(ContentType.TEXT)
                .when()
                .header("Authorization", "AUTH_HEADER")
                .get("/simple/apis")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT));
    }

    @Test
    public void testRequestWithParams() {
        given()
                .log().ifValidationFails()
                .accept(ContentType.TEXT)
                .when()
                .get("/simple/apis?param1=foo")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_ACCEPTED));
    }
}
