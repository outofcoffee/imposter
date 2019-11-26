package io.gatehill.imposter.plugin.openapi;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Paths;

import static com.jayway.restassured.RestAssured.given;

/**
 * Tests for complex path parameters.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ComplexPathParamsTest extends BaseVerticleTest {
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
        imposterConfig.setConfigDirs(new String[]{Paths.get(getClass().getResource("/complex-path-params").toURI()).toString()});
    }

    /**
     * Should return a sample artifact and  HTTP 200 status code, per behaviour script.
     *
     * @param testContext
     */
    @Test
    public void testServeDefaultExampleMatchContentType(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                .accept(ContentType.ANY)
                .when()
                .get("/apis/v1beta1/runs/1/nodes/2/artifacts/3:read")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        testContext.assertEquals("Example artifact data", body);
    }
}
