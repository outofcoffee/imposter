package io.gatehill.imposter.plugin.openapi;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;

/**
 * Tests for OpenAPI definitions with reference responses.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class DefaultResponseTest extends BaseVerticleTest {
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
                "/openapi3/default-response"
        );
    }

    /**
     * Should return example from reference response.
     *
     * @param testContext
     */
    @Test
    public void testReferenceObjectExample(TestContext testContext) {
        final JsonPath body = given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get("/v1/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().jsonPath();

        testContext.assertEquals(99, body.get("code"));
        testContext.assertEquals("Default response", body.get("message"));
    }
}
