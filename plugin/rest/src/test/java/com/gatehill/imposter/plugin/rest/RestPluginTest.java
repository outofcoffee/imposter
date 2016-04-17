package com.gatehill.imposter.plugin.rest;

import com.gatehill.imposter.plugin.Plugin;
import com.gatehill.imposter.server.BaseVerticleTest;
import com.jayway.restassured.RestAssured;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;


/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
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
    public void testRequestSuccess() throws Exception {
        given().when()
                .get("/example")
                .then()
                .statusCode(equalTo(HttpURLConnection.HTTP_OK))
                .and()
                .contentType(equalTo("application/json"))
                .and()
                .body("testKey", equalTo("testValue1"));
    }

    @Test
    public void testRequestScriptedResponseFile() throws Exception {
        // default action should return static data file 1
        given().when()
                .get("/scripted")
                .then()
                .statusCode(equalTo(HttpURLConnection.HTTP_OK))
                .and()
                .contentType(equalTo("application/json"))
                .and()
                .body("testKey", equalTo("testValue1"));

        // default action should return static data file 2
        given().when()
                .get("/scripted?action=fetch")
                .then()
                .statusCode(equalTo(HttpURLConnection.HTTP_OK))
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
                .statusCode(equalTo(HttpURLConnection.HTTP_CREATED))
                .and()
                .body(is(isEmptyOrNullString()));

        // script causes short circuit to 204
        given().when()
                .get("/scripted?action=delete")
                .then()
                .statusCode(equalTo(HttpURLConnection.HTTP_NO_CONTENT))
                .and()
                .body(is(isEmptyOrNullString()));

        // script causes short circuit to 400
        given().when()
                .get("/scripted?bad")
                .then()
                .statusCode(equalTo(HttpURLConnection.HTTP_BAD_REQUEST))
                .and()
                .body(is(isEmptyOrNullString()));
    }

    @Test
    public void testRequestNotFound() throws Exception {
        given().when()
                .get("/nonExistentEndpoint")
                .then()
                .statusCode(equalTo(HttpURLConnection.HTTP_NOT_FOUND));
    }
}
