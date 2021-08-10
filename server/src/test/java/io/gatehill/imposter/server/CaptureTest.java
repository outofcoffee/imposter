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
 * Tests for item capture.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class CaptureTest extends BaseVerticleTest {
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
                "/capture"
        );
    }

    /**
     * Capture request header attributes into a store.
     */
    @Test
    public void testCaptureHeaderItems() {
        // send data for capture
        given().when()
                .log().ifValidationFails()
                .pathParam("userId", "foo")
                .queryParam("page", 2)
                .header("X-Correlation-ID", "abc123")
                .get("/users/{userId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        // retrieve via system
        given().when()
                .log().ifValidationFails()
                .pathParam("storeId", "captureTest")
                .pathParam("key", "userId")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("foo"));

        given().when()
                .log().ifValidationFails()
                .pathParam("storeId", "captureTest")
                .pathParam("key", "page")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("2"));

        given().when()
                .log().ifValidationFails()
                .pathParam("storeId", "captureTest")
                .pathParam("key", "correlationId")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("abc123"));
    }

    /**
     * Capture request body properties into a store.
     */
    @Test
    public void testCaptureBodyItems() throws Exception {
        final String user = FileUtils.readFileToString(
                new File(CaptureTest.class.getResource("/capture/user.json").toURI()),
                StandardCharsets.UTF_8
        );

        // send data for capture
        given().when()
                .log().ifValidationFails()
                .body(user)
                .contentType(ContentType.JSON)
                .post("/users")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        // retrieve via system
        given().when()
                .log().ifValidationFails()
                .pathParam("storeId", "captureTest")
                .pathParam("key", "name")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("Alice"));

        given().when()
                .log().ifValidationFails()
                .pathParam("storeId", "captureTest")
                .pathParam("key", "postCode")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("PO5 7CO"));
    }
}
