package io.gatehill.imposter.plugin.openapi;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;

/**
 * Tests for returning specific status codes from OpenAPI mocks.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OverrideStatusCodeTest extends BaseVerticleTest {
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
                "/openapi3/override-status-code"
        );
    }

    /**
     * Should return a specific status code for a simple request path.
     */
    @Test
    public void testOverrideStatusCodesForSimplePath(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .body("{ \"id\": 1, \"name\": \"Cat\" }")
                .post("/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(201);
    }

    /**
     * Should return a specific status code for a path parameter.
     */
    @Test
    public void testOverrideStatusCodesForPathParam(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get("/pets/{petId}", "1")
                .then()
                .log().ifValidationFails()
                .statusCode(203);
    }

    /**
     * Should return a specific status code for a query parameter.
     */
    @Test
    public void testOverrideStatusCodesForQueryParam(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get("/pets?foo=bar")
                .then()
                .log().ifValidationFails()
                .statusCode(204);
    }

    /**
     * Should return a specific status code for a path with a placeholder.
     */
    @Test
    public void testOverrideStatusCodesForPathWithPlaceholder(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .body("{ \"id\": 1, \"name\": \"Cat\" }")
                .put("/pets/1")
                .then()
                .log().ifValidationFails()
                .statusCode(202);
    }
}
