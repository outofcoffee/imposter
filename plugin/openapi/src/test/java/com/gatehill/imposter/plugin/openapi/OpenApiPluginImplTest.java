package com.gatehill.imposter.plugin.openapi;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.server.BaseVerticleTest;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;
import sun.net.www.protocol.http.HttpURLConnection;

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

    /**
     * Should return the example from the specification for the default HTTP 200 status code.
     *
     * @param testContext
     * @throws Exception
     */
    @Test
    public void testServeDefaultExample(TestContext testContext) throws Exception {
        final String body = given()
                .log().everything()
                .accept(ContentType.JSON)
                .when()
                .get("/apis")
                .then()
                .log().everything()
                .statusCode(HttpURLConnection.HTTP_OK)
                .extract().asString();

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
     * Should return the example from the specification when the script triggers an HTTP 201 Created status code.
     *
     * @throws Exception
     */
    @Test
    public void testServeScriptedExample() throws Exception {
        given()
                .log().everything()
                .accept(ContentType.JSON)
                .when()
                .put("/apis")
                .then()
                .log().everything()
                .statusCode(HttpURLConnection.HTTP_CREATED)
                .body("result", equalTo("success"));
    }
}
