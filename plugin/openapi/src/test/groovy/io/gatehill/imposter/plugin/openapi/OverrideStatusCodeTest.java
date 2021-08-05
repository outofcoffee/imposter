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
    public void testSetStatusCodeForSimplePath(TestContext testContext) {
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
    public void testSetStatusCodeForPathParam(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get("/pets/{petId}", "99")
                .then()
                .log().ifValidationFails()
                .statusCode(203);
    }

    /**
     * Should return a specific status code for a query parameter.
     */
    @Test
    public void testSetStatusCodeForQueryParam(TestContext testContext) {
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
     * Should return a specific status code for a request header.
     */
    @Test
    public void testSetStatusCodeForRequestHeader(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .when()
                .header("X-Foo", "bar")
                .get("/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(205);
    }

    /**
     * Should return a specific status code for a request header, where
     * the case of the request header key differs from that of the configuration.
     */
    @Test
    public void testSetStatusCodeForRequestHeaderCaseInsensitive(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .when()
                // header key deliberately uppercase in request, but lowercase in config
                .header("X-LOWERCASE-TEST", "baz")
                .body("{ \"id\": 1, \"name\": \"Cat\" }")
                .put("/pets/{petId}", "1")
                .then()
                .log().ifValidationFails()
                .statusCode(409);
    }

    /**
     * Should return a specific status code for a path with a placeholder.
     */
    @Test
    public void testSetStatusCodeForPathWithPlaceholder(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .body("{ \"id\": 1, \"name\": \"Cat\" }")
                .put("/pets/{petId}", "1")
                .then()
                .log().ifValidationFails()
                .statusCode(202);
    }
}
