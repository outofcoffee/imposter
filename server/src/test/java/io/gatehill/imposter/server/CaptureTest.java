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

package io.gatehill.imposter.server;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.test.TestPluginImpl;
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
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;

/**
 * Tests for item capture.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner.class)
public class CaptureTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);
        RestAssured.baseURI = "http://" + getHost() + ":" + getListenPort();
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
                .pathParam("userId", "foo")
                .queryParam("page", 2)
                .header("X-Correlation-ID", "abc123")
                .get("/users/{userId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        // retrieve via system
        given().when()
                .pathParam("storeId", "captureTest")
                .pathParam("key", "userId")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("foo"));

        given().when()
                .pathParam("storeId", "captureTest")
                .pathParam("key", "page")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("2"));

        given().when()
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
                .body(user)
                .contentType(ContentType.JSON)
                .post("/users")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        // retrieve via system
        given().when()
                .pathParam("storeId", "captureTest")
                .pathParam("key", "name")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("Alice"));

        given().when()
                .pathParam("storeId", "captureTest")
                .pathParam("key", "postCode")
                .get("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("PO5 7CO"));
    }

    /**
     * Capture a constant value into a store with a dynamic key.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testCaptureConstWithDynamicKey() {
        // send data for capture
        given().when()
                .pathParam("userId", "alice")
                .put("/users/admins/{userId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        given().when()
                .pathParam("userId", "bob")
                .put("/users/admins/{userId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        // retrieve via system
        final Map<String, ?> body = given().when()
                .pathParam("storeId", "captureTestAdmins")
                .get("/system/store/{storeId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .contentType(ContentType.JSON)
                .extract().body().as(Map.class);

        assertThat(body.entrySet(), hasSize(2));
        assertThat(body, allOf(
                hasEntry("alice", "admin"),
                hasEntry("bob", "admin")
        ));
    }
}
