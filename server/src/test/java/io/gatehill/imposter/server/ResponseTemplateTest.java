package io.gatehill.imposter.server;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.test.TestPluginImpl;
import io.gatehill.imposter.server.util.FeatureUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for storage.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class ResponseTemplateTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        // enable store support before boot
        System.setProperty(FeatureUtil.SYS_PROP_IMPOSTER_FEATURES, "stores");

        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/response-template"
        );
    }

    /**
     * Interpolate a template placeholder using a store value.
     */
    @Test
    public void testReadInterpolatedTemplate() {
        // create item
        given().when()
                .pathParam("storeId", "templateTest")
                .pathParam("key", "foo")
                .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                .body("bar")
                .put("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED));

        // read interpolated response
        given().when()
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("Hello bar!"));
    }
}
