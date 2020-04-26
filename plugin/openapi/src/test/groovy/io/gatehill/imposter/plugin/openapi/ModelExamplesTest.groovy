package io.gatehill.imposter.plugin.openapi

import com.google.common.collect.Lists
import com.jayway.restassured.RestAssured
import com.jayway.restassured.http.ContentType
import io.gatehill.imposter.plugin.Plugin
import io.gatehill.imposter.server.BaseVerticleTest
import io.gatehill.imposter.util.HttpUtil
import io.vertx.core.json.JsonArray
import io.vertx.ext.unit.TestContext
import org.junit.Before
import org.junit.Test

import static com.jayway.restassured.RestAssured.given

/**
 * Tests for model examples.
 *
 * @author benjvoigt
 */
class ModelExamplesTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return OpenApiPluginImpl.class
    }

    @Before
    void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext)
        RestAssured.baseURI = "http://$HOST:$listenPort"
    }

    @Override
    protected List<String> getTestConfigDirs() {
        Lists.newArrayList(
                '/openapi2/model-examples'
        )
    }

    @Test
    void testServeModelExamples(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                .accept(ContentType.JSON) // JSON content type in 'Accept' header matches specification example
                .when()
                .get('/api/pets')
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString()

        final JsonArray actual = new JsonArray(body)
        final JsonArray expected = new JsonArray("""
        [
            {
                "name": "",
                "id": 0,
                "breed": "Collie",
                "misc": {
                    "nocturnal": false,
                    "population": 47435
                }
            }
        ]
        """)

        testContext.assertEquals(expected, actual)
    }
}
