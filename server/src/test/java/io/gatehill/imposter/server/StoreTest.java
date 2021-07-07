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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

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
        System.setProperty(FeatureUtil.SYS_PROP_IMPOSTER_FEATURES, "stores");

        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
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
    public void testSetAndGetFromStoreScripted() {
        // save via script
        given().when()
                .queryParam("foo", "qux")
                .put("/store")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED));

        // load via script
        given().when()
                .get("/load")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("qux"));
    }

    /**
     * Fail to load a nonexistent store.
     */
    @Test
    public void testNonexistentStore() {
        given().when()
                .pathParam("storeId", "nonexistent")
                .get("/system/store/{storeId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NOT_FOUND));
    }

    /**
     * Fail to load a store with an incorrect Accept header.
     */
    @Test
    public void testUnacceptableMimeType() {
        // populate the store
        given().when()
                .pathParam("storeId", "umt")
                .pathParam("key", "foo")
                .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                .body("baz")
                .put("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED));

        // incorrect mime type
        given().when()
                .pathParam("storeId", "umt")
                .accept(ContentType.XML)
                .get("/system/store/{storeId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NOT_ACCEPTABLE));

        // correct mime type
        given().when()
                .pathParam("storeId", "umt")
                .accept(ContentType.JSON)
                .get("/system/store/{storeId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }

    /**
     * Save and load from the store across multiple requests.
     */
    @Test
    public void testSetAndGetSingleFromStore() {
        // initially empty
        given().when()
                .pathParam("storeId", "sgs")
                .pathParam("key", "bar")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NOT_FOUND));

        // create via system
        given().when()
                .pathParam("storeId", "sgs")
                .pathParam("key", "bar")
                .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                .body("corge")
                .put("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED));

        // update via system
        given().when()
                .pathParam("storeId", "sgs")
                .pathParam("key", "bar")
                .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                .body("quux")
                .put("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        given().when()
                .pathParam("storeId", "sgs")
                .pathParam("key", "bar")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("quux"));
    }

    /**
     * Save and load from the store across multiple requests.
     */
    @Test
    public void testSetAndGetMultipleFromStore() {
        // initially empty
        given().when()
                .pathParam("storeId", "sgm")
                .get("/system/store/{storeId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NOT_FOUND));

        final Map<String, Object> body = new HashMap<String, Object>() {{
            put("baz", "quuz");
            put("corge", "grault");
        }};

        // save via system
        given().when()
                .pathParam("storeId", "sgm")
                .contentType(ContentType.JSON)
                .body(body)
                .post("/system/store/{storeId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        // load all
        given().when()
                .pathParam("storeId", "sgm")
                .get("/system/store/{storeId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body("$", allOf(
                        hasEntry("baz", "quuz"),
                        hasEntry("corge", "grault")
                ));
    }

    /**
     * Delete an item from a store.
     */
    @Test
    public void testDeleteFromStore() {
        // save via system
        given().when()
                .pathParam("storeId", "ditem")
                .pathParam("key", "corge")
                .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                .body("quux")
                .put("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED));

        given().when()
                .pathParam("storeId", "ditem")
                .pathParam("key", "corge")
                .delete("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT));

        // should not exist
        given().when()
                .pathParam("storeId", "ditem")
                .pathParam("key", "corge")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NOT_FOUND));
    }

    /**
     * Save and load from the store across multiple requests.
     */
    @Test
    public void testSetAndGetAllFromStore() {
        // save via system
        given().when()
                .pathParam("storeId", "sga")
                .pathParam("key", "baz")
                .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                .body("quuz")
                .put("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED));

        // load all
        given().when()
                .pathParam("storeId", "sga")
                .get("/system/store/{storeId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body("$", hasEntry("baz", "quuz"));
    }

    /**
     * Save and load from the store across multiple requests.
     */
    @Test
    public void testDeleteStore() {
        // save via system
        given().when()
                .pathParam("storeId", "dstore")
                .pathParam("key", "baz")
                .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                .body("quuz")
                .put("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED));

        // delete
        given().when()
                .pathParam("storeId", "dstore")
                .delete("/system/store/{storeId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT));

        // should not exist
        given().when()
                .pathParam("storeId", "dstore")
                .get("/system/store/{storeId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NOT_FOUND));
    }
}
