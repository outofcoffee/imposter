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
 * Tests for storage.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class StoreTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        // enable store support before boot
        System.setProperty("imposter.experimental", "store");

        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/store"
        );
    }

    /**
     * Save and load from the store across multiple requests.
     */
    @Test
    public void testSetAndGetFromStore() {
        given().when()
                .queryParam("foo", "bar")
                .get("/store")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED));

        given().when()
                .get("/load")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("bar"));
    }
}
