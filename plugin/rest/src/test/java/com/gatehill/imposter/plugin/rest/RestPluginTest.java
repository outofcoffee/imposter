package com.gatehill.imposter.plugin.rest;

import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.server.BaseVerticleTest;
import com.gatehill.imposter.util.HttpUtil;
import com.jayway.restassured.RestAssured;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return RestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);

        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
    }

    @Test
    public void testRequestRootPathSuccess() throws Exception {
        given().when()
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .and()
                .contentType(equalTo("application/json"))
                .and()
                .body("testKey", equalTo("testValue1"));
    }

    @Test
    public void testRequestArrayResourceSuccess() throws Exception {
        fetchVerifyRow(1);
        fetchVerifyRow(2);
        fetchVerifyRow(3);
    }

    private void fetchVerifyRow(final int rowId) {
        given().when()
                .get("/example/" + rowId)
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .and()
                .contentType(equalTo("application/json"))
                .and()
                .body("aKey", equalTo("aValue" + rowId));
    }

    @Test
    public void testRequestScriptedResponseFile() throws Exception {
        // default action should return static data file 1
        given().when()
                .get("/scripted")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .and()
                .contentType(equalTo("application/json"))
                .and()
                .body("testKey", equalTo("testValue1"));

        // default action should return static data file 2
        given().when()
                .get("/scripted?action=fetch")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .and()
                .contentType(equalTo("application/json"))
                .and()
                .body("testKey", equalTo("testValue2"));
    }

    @Test
    public void testRequestScriptedStatusCode() throws Exception {
        // script causes short circuit to 201
        given().when()
                .get("/scripted?action=create")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED))
                .and()
                .body(is(isEmptyOrNullString()));

        // script causes short circuit to 204
        given().when()
                .get("/scripted?action=delete")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT))
                .and()
                .body(is(isEmptyOrNullString()));

        // script causes short circuit to 400
        given().when()
                .get("/scripted?bad")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_BAD_REQUEST))
                .and()
                .body(is(isEmptyOrNullString()));
    }

    @Test
    public void testRequestNotFound() throws Exception {
        given().when()
                .get("/nonExistentEndpoint")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_NOT_FOUND));
    }
}
