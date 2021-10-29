/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

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
 * @author Pete Cornish
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
