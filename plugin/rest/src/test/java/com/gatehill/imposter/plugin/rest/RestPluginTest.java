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
                .body("testKey", equalTo("testValue"));
    }

    @Test
    public void testRequestNotFound() throws Exception {
        given().when()
                .get("/nonExistentEndpoint")
                .then()
                .statusCode(equalTo(HttpURLConnection.HTTP_NOT_FOUND));
    }
}
