package io.gatehill.imposter.plugin.openapi;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.vertx.ext.unit.TestContext;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * Tests for request validation for OpenAPI mocks.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RequestValidationTest extends BaseVerticleTest {
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
                "/openapi3/request-validation"
        );
    }

    /**
     * Request should pass request validation.
     */
    @Test
    public void testValidRequest(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .header("X-CorrelationID", "foo")
                .body("{ \"id\": 1, \"name\": \"Cat\" }")
                .post("/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(201);
    }

    /**
     * Request should fail request validation due to nonconformant request body.
     */
    @Test
    public void testMissingRequestBody(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .header("X-CorrelationID", "foo")
                .post("/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(containsString("A request body is required but none found"));
    }

    /**
     * Request should fail request validation due to nonconformant request body.
     */
    @Test
    public void testInvalidRequestBody(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .header("X-CorrelationID", "foo")
                .body("{ \"invalid\": \"request\" }")
                .post("/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(
                        containsString("Object instance has properties which are not allowed by the schema"),
                        containsString("Object has missing required properties ([\"id\",\"name\"])")
                );
    }

    /**
     * Request should fail request validation due to missing header.
     */
    @Test
    public void testMissingHeader(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .body("{ \"id\": 1, \"name\": \"Cat\" }")
                .post("/pets")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(containsString("Header parameter 'X-CorrelationID' is required on path '/pets' but not found in request"));
    }

    /**
     * Request should fail request validation due to nonconformant path parameter.
     */
    @Test
    public void testInvalidPathParameter(TestContext testContext) {
        given()
                .log().ifValidationFails()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .when()
                .body("{ \"id\": 1, \"name\": \"Cat\" }")
                .put("/pets/invalidId")
                .then()
                .log().ifValidationFails()
                .statusCode(400)
                .body(containsString("Instance type (string) does not match any allowed primitive type (allowed: [\"integer\"])"));
    }
}
