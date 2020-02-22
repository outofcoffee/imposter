package io.gatehill.imposter.plugin.openapi;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static com.jayway.restassured.RestAssured.given;

/**
 * Tests for OpenAPI definitions with object examples.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ObjectExamplesTest extends BaseVerticleTest {
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
    protected void configure(ImposterConfig imposterConfig) throws Exception {
        super.configure(imposterConfig);
        imposterConfig.setConfigDirs(new String[]{Paths.get(getClass().getResource("/object-examples").toURI()).toString()});
    }

    /**
     * Should return object example formatted as JSON.
     *
     * @param testContext
     */
    @Test
    public void testObjectExample(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get("/objects/team")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        final String trimmed = body.trim();
        testContext.assertTrue(trimmed.startsWith("{"));
        testContext.assertTrue(trimmed.contains("Engineering"));
        testContext.assertTrue(trimmed.endsWith("}"));
    }
}
