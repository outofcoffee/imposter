package io.gatehill.imposter.server;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.test.TestPluginImpl;
import io.gatehill.imposter.server.util.FeatureUtil;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for response templates.
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
     * Interpolate a simple template placeholder using a store value.
     */
    @Test
    public void testSimpleInterpolatedTemplate() {
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
                .body(equalTo("Hello bar!"))
                // content type inferred from response file name
                .contentType(ContentType.TEXT);
    }

    /**
     * Interpolate a request-scoped template placeholder.
     */
    @Test
    public void testRequestScopedInterpolatedTemplate() {
        given().when()
                .contentType(ContentType.JSON)
                .pathParam("petId", 99)
                .put("/pets/{petId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("Pet ID: 99"))
                // content type inferred from response file name
                .contentType(ContentType.TEXT);
    }

    /**
     * Interpolate a JsonPath template placeholder using a store value.
     */
    @Test
    public void testJsonPathInterpolatedTemplate() throws Exception {
        final String user = FileUtils.readFileToString(
                new File(CaptureTest.class.getResource("/response-template/user.json").toURI()),
                StandardCharsets.UTF_8
        );

        given().when()
                .body(user)
                .contentType(ContentType.JSON)
                .post("/users")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("Postcode: PO5 7CO"))
                // content type inferred from response file name
                .contentType(ContentType.TEXT);
    }
}
