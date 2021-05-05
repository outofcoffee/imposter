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
import org.yaml.snakeyaml.Yaml

import static com.jayway.restassured.RestAssured.given

/**
 * Tests for schema examples.
 *
 * @author benjvoigt
 */
class SchemaExamplesTest extends BaseVerticleTest {
    private static final Yaml YAML_PARSER = new Yaml();

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
    void testServeSchemaExamplesAsJson(TestContext testContext) {
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
                "bornAt" : "2015-02-01T08:00:00Z",
                "lastVetVisitOn": "2020-03-15",
                "misc": {
                    "nocturnal": false,
                    "population": 47435
                }
            }
        ]
        """)

        testContext.assertEquals(expected, actual)
    }

    @Test
    void testServeSchemaExamplesAsYaml(TestContext testContext) {
        final String rawBody = given()
                .log().ifValidationFails()
                .accept("application/x-yaml") // YAML content type in 'Accept' header matches specification example
                .when()
                .get('/api/pets')
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString()

        final List<Map<String, ?>> yamlBody = YAML_PARSER.load(rawBody);
        testContext.assertEquals(1, yamlBody.size());

        final first = yamlBody.first()
        testContext.assertEquals("", first.get("name"));
        testContext.assertEquals(0, first.get("id"));
        testContext.assertEquals("Collie", first.get("breed"));
        testContext.assertEquals("2015-02-01T08:00:00Z", first.get("bornAt"));
        testContext.assertEquals("2020-03-15", first.get("lastVetVisitOn"));

        final misc = first.get("misc") as Map<String, ?>
        testContext.assertNotNull(misc, "misc property should not be null");
        testContext.assertEquals(false, misc.get("nocturnal"));
        testContext.assertEquals(47435, misc.get("population"));
    }
}
