package io.gatehill.imposter.plugin.openapi;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for examples referenced using '$ref' in spec.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ExampleRefTest extends BaseVerticleTest {
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
                "/openapi3/example-ref"
        );
    }

    /**
     * Expects that different request URIs return specific request examples.
     */
    @Test
    public void testExampleRefReturned() {
        given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get("/pets/1")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .body("id", equalTo(1))
                .body("name", equalTo("Cat"));
    }
}
