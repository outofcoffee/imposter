package com.gatehill.imposter.plugin.openapi;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.server.BaseVerticleTest;
import com.gatehill.imposter.util.HttpUtil;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

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

    @Override
    protected void configure(ImposterConfig imposterConfig) throws Exception {
        super.configure(imposterConfig);
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
     * @throws Exception
     */
    @Test
    public void testServeDefaultExampleMatchContentType(TestContext testContext) throws Exception {
        final String body = given()
                .log().everything()
                // JSON content type in 'Accept' header matches specification example
                .accept(ContentType.JSON)
                .when()
                .get("/simple/apis")
                .then()
                .log().everything()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        assertBody(testContext, body);
    }

    /**
     * Should return the example from the specification when the script triggers an HTTP 201 Created status code.
     *
     * @throws Exception
     */
    @Test
    public void testServeScriptedExample() throws Exception {
        given()
                .log().everything()
                // JSON content type in 'Accept' header matches specification example
                .accept(ContentType.JSON)
                .when()
                .put("/simple/apis")
                .then()
                .log().everything()
                .statusCode(HttpUtil.HTTP_CREATED)
                .body("result", equalTo("success"))
                .header("MyHeader","MyHeaderValue");
    }

    /**
     * Should return the example from the specification for the default HTTP 200 status code, even though the
     * content type in the 'Accept' header does not match that in the specification example.
     *
     * @param testContext
     * @throws Exception
     * @see OpenApiPluginConfig#isPickFirstIfNoneMatch()
     */
    @Test
    public void testServeDefaultExampleNoExactMatch(TestContext testContext) throws Exception {
        final String body = given()
                .log().everything()
                // do not set JSON content type in 'Accept' header, to force mismatch against specification example
                .accept(ContentType.TEXT)
                .when()
                .get("/simple/apis")
                .then()
                .log().everything()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        assertBody(testContext, body);
    }

    /**
     * Should return the specification UI.
     *
     * @param testContext
     * @throws Exception
     */
    @Test
    public void testGetSpecUi(TestContext testContext) throws Exception {
        final String body = given()
                .log().everything()
                .accept(ContentType.TEXT)
                .when()
                .get(OpenApiPluginImpl.SPECIFICATION_PATH + "/")
                .then()
                .log().everything()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        testContext.assertTrue(body.contains("<html>"));
    }

    /**
     * Should return a combined specification.
     *
     * @param testContext
     * @throws Exception
     */
    @Test
    public void testGetCombinedSpec(TestContext testContext) throws Exception {
        final String body = given()
                .log().everything()
                .accept(ContentType.JSON)
                .when()
                .get(OpenApiPluginImpl.COMBINED_SPECIFICATION_PATH)
                .then()
                .log().everything()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        testContext.assertNotNull(body);

        final Swagger combined = new SwaggerParser().parse(body);
        testContext.assertNotNull(combined.getInfo());
        testContext.assertEquals("Imposter Mock APIs", combined.getInfo().getTitle());

        // should contain combination of both specs' endpoints
        testContext.assertEquals(4, combined.getPaths().size());
        testContext.assertTrue(combined.getPaths().keySet().contains("/simple/apis"));
        testContext.assertTrue(combined.getPaths().keySet().contains("/api/pets"));
    }

    @Test
    public void testRequestWithHeaders() throws Exception {
        given()
            .log().everything()
            .accept(ContentType.TEXT)
            .when()
            .header("Authorization", "AUTH_HEADER")
            .get("/simple/apis")
            .then()
            .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT));
    }
}
