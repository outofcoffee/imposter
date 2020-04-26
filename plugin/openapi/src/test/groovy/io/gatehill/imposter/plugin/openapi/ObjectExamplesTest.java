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
import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;

/**
 * Tests for OpenAPI definitions with object examples.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ObjectExamplesTest extends BaseVerticleTest {
    private static final Yaml YAML_PARSER = new Yaml();

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
                "/openapi2/object-examples"
        );
    }

    /**
     * Should return object example formatted as JSON.
     */
    @Test
    public void testObjectExampleAsJson(TestContext testContext) {
        final JsonPath body = given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get("/objects/team")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().jsonPath();

        testContext.assertEquals(10, body.get("id"));
        testContext.assertEquals("Engineering", body.get("name"));
    }

    /**
     * Should return object example formatted as YAML.
     */
    @Test
    public void testObjectExampleAsYaml(TestContext testContext) {
        final String rawBody = given()
                .log().ifValidationFails()
                .accept("application/x-yaml")
                .when()
                .get("/objects/team")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        final Map<String, ?> yamlBody = YAML_PARSER.load(rawBody);
        testContext.assertEquals(20, yamlBody.get("id"));
        testContext.assertEquals("Product", yamlBody.get("name"));
    }
}
