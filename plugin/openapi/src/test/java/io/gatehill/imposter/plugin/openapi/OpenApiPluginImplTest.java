package io.gatehill.imposter.plugin.openapi;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.gatehill.imposter.util.HttpUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.function.Consumer;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for {@link OpenApiPluginImpl}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginImplTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return OpenApiPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
    }

    private void assertBody(TestContext testContext, String body) {
        testContext.assertNotNull(body);

        final JsonObject jsonBody = new JsonObject(body);
        final JsonArray versions = jsonBody.getJsonArray("versions");
        testContext.assertNotNull(versions, "Versions array should exist");
        testContext.assertEquals(2, versions.size());

        // verify entries
        testContext.assertNotNull(versions.getJsonObject(0), "Version array entry 0 should exist");
        testContext.assertNotNull(versions.getJsonObject(1), "Version array entry 1 should exist");
    }

    /**
     * Should return the example from the specification for the default HTTP 200 status code, since the
     * content type in the 'Accept' matches that in the specification example.
     *
     * @param testContext
     */
    @Test
    public void testServeDefaultExampleMatchContentType(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                // JSON content type in 'Accept' header matches specification example
                .accept(ContentType.JSON)
                .when()
                .get("/simple/apis")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        assertBody(testContext, body);
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

    /**
     * Should return the example from the specification for the default HTTP 200 status code, even though the
     * content type in the 'Accept' header does not match that in the specification example.
     *
     * @param testContext
     * @see OpenApiPluginConfig#isPickFirstIfNoneMatch()
     */
    @Test
    public void testServeDefaultExampleNoExactMatch(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                // do not set JSON content type in 'Accept' header, to force mismatch against specification example
                .accept(ContentType.TEXT)
                .when()
                .get("/simple/apis")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        assertBody(testContext, body);
    }

    /**
     * Should return the specification UI.
     *
     * @param testContext
     */
    @Test
    public void testGetSpecUi(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                .accept(ContentType.TEXT)
                .when()
                .get(OpenApiPluginImpl.SPECIFICATION_PATH + "/")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        testContext.assertTrue(body.contains("</html>"));
    }

    /**
     * Should return a combined specification.
     *
     * @param testContext
     */
    @Test
    public void testGetCombinedSpec(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get(OpenApiPluginImpl.COMBINED_SPECIFICATION_PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        testContext.assertNotNull(body);

        final SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(body, Collections.emptyList(), new ParseOptions());
        testContext.assertNotNull(parseResult);

        final OpenAPI combined = parseResult.getOpenAPI();
        testContext.assertNotNull(combined);

        testContext.assertNotNull(combined.getInfo());
        testContext.assertEquals("Imposter Mock APIs", combined.getInfo().getTitle());

        // should contain combination of all specs' endpoints
        testContext.assertEquals(7, combined.getPaths().size());

        // OASv2
        testContext.assertTrue(combined.getPaths().containsKey("/apis"));
        testContext.assertTrue(combined.getPaths().containsKey("/v2"));
        testContext.assertTrue(combined.getPaths().containsKey("/pets"));
        testContext.assertTrue(combined.getPaths().containsKey("/pets/{id}"));
        testContext.assertTrue(combined.getPaths().containsKey("/team"));

        // OASv3
        testContext.assertTrue(combined.getPaths().containsKey("/oas3/apis"));
        testContext.assertTrue(combined.getPaths().containsKey("/oas3/v2"));
    }

    /**
     * Should return examples formatted as JSON.
     *
     * @param testContext
     */
    @Test
    public void testExamples(TestContext testContext) {
        // OASv2
        queryEndpoint("/simple/apis", responseBody -> {
            final String trimmed = responseBody.trim();
            testContext.assertTrue(trimmed.startsWith("{"));
            testContext.assertTrue(trimmed.contains("CURRENT"));
            testContext.assertTrue(trimmed.endsWith("}"));
        });

        queryEndpoint("/api/pets/1", responseBody -> {
            final String trimmed = responseBody.trim();
            testContext.assertTrue(trimmed.startsWith("{"));
            testContext.assertTrue(trimmed.contains("Fluffy"));
            testContext.assertTrue(trimmed.endsWith("}"));
        });

        // OASv3
        queryEndpoint("/oas3/apis", responseBody -> {
            final String trimmed = responseBody.trim();
            testContext.assertTrue(trimmed.startsWith("{"));
            testContext.assertTrue(trimmed.contains("CURRENT"));
            testContext.assertTrue(trimmed.endsWith("}"));
        });

        // object example
        queryEndpoint("/objects/team", responseBody -> {
            final String trimmed = responseBody.trim();
            testContext.assertTrue(trimmed.startsWith("{"));
            testContext.assertTrue(trimmed.contains("Engineering"));
            testContext.assertTrue(trimmed.endsWith("}"));
        });
    }

    private void queryEndpoint(String url, Consumer<String> bodyConsumer) {
        final String body = given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get(url)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        bodyConsumer.accept(body);
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
}
