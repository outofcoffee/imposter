package io.gatehill.imposter.server;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.test.TestPluginImpl;
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
 * Tests for preloading data into storage subsystem.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class StorePreloadTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/store-preload"
        );
    }

    /**
     * Fetch data preloaded from a file.
     */
    @Test
    public void testFetchPreloadedFile() {
        // simple value
        given().when()
                .pathParam("storeId", "preload-file-test")
                .pathParam("key", "foo")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("bar"));

        // object value
        given().when()
                .pathParam("storeId", "preload-file-test")
                .pathParam("key", "baz")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body("qux", equalTo("corge"));
    }

    /**
     * Fetch data preloaded from inline config.
     */
    @Test
    public void testFetchPreloadedInlineData() {
        // simple value
        given().when()
                .pathParam("storeId", "preload-data-test")
                .pathParam("key", "foo")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("bar"));

        // object value
        given().when()
                .pathParam("storeId", "preload-data-test")
                .pathParam("key", "baz")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body("qux", equalTo("corge"));
    }
}
