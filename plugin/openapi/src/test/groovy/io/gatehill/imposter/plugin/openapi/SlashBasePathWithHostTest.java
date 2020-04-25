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
 * Tests for OpenAPI definitions with a single '/' as the base path and a specified host.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SlashBasePathWithHostTest extends BaseVerticleTest {
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
                "/openapi2/slash-base-path-with-host"
        );
    }

    /**
     * Should return the example.
     *
     * @param testContext
     */
    @Test
    public void testSpec(TestContext testContext) {
        final Map<String, Object> paths = given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get("/_spec/combined.json")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().jsonPath().getMap("paths");

        testContext.assertEquals(1, paths.size());
        testContext.assertTrue(paths.containsKey("/pets"));
    }

    /**
     * Should return the example.
     *
     * @param testContext
     */
    @Test
    public void testExample(TestContext testContext) {
        final JsonPath body = given()
                .log().ifValidationFails()
                .accept(ContentType.ANY)
                .when()
                .get("/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().jsonPath();

        final List<Object> pets = body.getList("");
        testContext.assertEquals(2, pets.size());
    }
}
