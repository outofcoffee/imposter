package com.gatehill.imposter.plugin.rest;

import com.gatehill.imposter.server.ImposterVerticle;
import com.jayway.restassured.RestAssured;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.nio.file.Paths;

import static com.gatehill.imposter.server.ImposterVerticle.CONFIG_PREFIX;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;


/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class RestPluginTest {
    private static final int LISTEN_PORT = 8443;
    private static final String HOST = "localhost";

    @Rule
    public RunTestOnContext rule = new RunTestOnContext();

    @Before
    public void setUp(TestContext testContext) throws Exception {
        final Async async = testContext.async();

        System.setProperty(CONFIG_PREFIX + "configDir", Paths.get(RestPluginTest.class.getResource("/config").toURI()).toString());
        System.setProperty(CONFIG_PREFIX + "pluginClass", RestPluginImpl.class.getCanonicalName());
        System.setProperty(CONFIG_PREFIX + "host", HOST);
        System.setProperty(CONFIG_PREFIX + "listenPort", String.valueOf(LISTEN_PORT));

        rule.vertx().deployVerticle(ImposterVerticle.class.getCanonicalName(), completion -> {
            if (completion.succeeded()) {
                async.complete();
            } else {
                testContext.fail(completion.cause());
            }
        });

        RestAssured.baseURI = "http://" + HOST + ":" + LISTEN_PORT;
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
