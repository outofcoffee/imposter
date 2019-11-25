package io.gatehill.imposter.plugin.rest;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.config.RedirectConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import static com.jayway.restassured.RestAssured.given;
import static io.gatehill.imposter.util.HttpUtil.CONTENT_TYPE;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;


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
        RestAssured.config().redirect(RedirectConfig.redirectConfig().followRedirects(false));
    }

    @Test
    public void testRequestStaticRootPathSuccess() {
        given().when()
                .get("/example")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .and()
                .contentType(equalTo("application/json"))
                .and()
                .body("testKey", equalTo("testValue1"));
    }

    @Test
    public void testRequestStaticArrayResourceSuccess() {
        fetchVerifyRow(1);
        fetchVerifyRow(2);
        fetchVerifyRow(3);
    }

    private void fetchVerifyRow(final int rowId) {
        given().when()
                .get("/example/" + rowId)
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .and()
                .contentType(equalTo("application/json"))
                .and()
                .body("aKey", equalTo("aValue" + rowId));
    }

    @Test
    public void testRequestScriptedResponseFile() {
        // default action should return static data file 1
        given().when()
                .get("/scripted")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .and()
                .contentType(equalTo("application/json"))
                .and()
                .body("testKey", equalTo("testValue1"));

        // default action should return static data file 2
        given().when()
                .get("/scripted?action=fetch")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .and()
                .contentType(equalTo("application/json"))
                .and()
                .body("testKey", equalTo("testValue2"));
    }

    @Test
    public void testRequestScriptedStatusCode() {
        // script causes short circuit to 201
        given().when()
                .get("/scripted?action=create")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED))
                .and()
                .body(is(isEmptyOrNullString()));

        // script causes short circuit to 204
        given().when()
                .get("/scripted?action=delete")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT))
                .and()
                .body(is(isEmptyOrNullString()));

        // script causes short circuit to 400
        given().when()
                .get("/scripted?bad")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_BAD_REQUEST))
                .and()
                .body(is(isEmptyOrNullString()));
    }

    @Test
    public void testRequestNotFound() {
        given().when()
                .get("/nonExistentEndpoint")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_NOT_FOUND));
    }

    @Test
    public void testRequestScriptedWithHeaders() {
        given().when()
                .header("Authorization", "AUTH_HEADER")
                .get("/scripted?with-auth")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_NO_CONTENT));
    }

    /**
     * Tests status code, headers, static data and method for a single resource.
     */
    @Test
    public void testRequestStaticSingleFull() {
        given().when()
                .get("/static-single")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .header(CONTENT_TYPE, "text/html")
                .header("X-Example", "foo")
                .body(allOf(
                        containsString("<html>"),
                        containsString("Hello, world!")
                ));
    }

    /**
     * Tests status code, headers, static data and method for a single resource.
     */
    @Test
    public void testRequestStaticMultiFull() {
        given().when()
                .post("/static-multi")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_MOVED_TEMP))
                .body(isEmptyOrNullString());

        given().when()
                .get("/static-multi")
                .then()
                .log().ifValidationFails()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .header(CONTENT_TYPE, "text/html")
                .header("X-Example", "foo")
                .body(allOf(
                        containsString("<html>"),
                        containsString("Hello, world!")
                ));
    }
}
