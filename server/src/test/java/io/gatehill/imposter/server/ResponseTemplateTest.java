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

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests for response templates.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner.class)
public class ResponseTemplateTest extends BaseVerticleTest {
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
                "/response-template"
        );
    }

    /**
     * Interpolate a simple template placeholder in a file using a store value.
     */
    @Test
    public void testSimpleInterpolatedTemplateFromFile() {
        // create item
        given().when()
                .pathParam("storeId", "templateTest")
                .pathParam("key", "foo")
                .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                .body("bar")
                .put("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED));

        // read interpolated response
        given().when()
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("Hello bar!"))
                // content type inferred from response file name
                .contentType(ContentType.TEXT);
    }

    /**
     * Interpolate a simple template placeholder from inline data using a store value.
     */
    @Test
    public void testSimpleInterpolatedTemplateFromInlineData() {
        // create item
        given().when()
                .pathParam("storeId", "templateTest")
                .pathParam("key", "foo-inline")
                .contentType(HttpUtil.CONTENT_TYPE_PLAIN_TEXT)
                .body("bar")
                .put("/system/store/{storeId}/{key}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_CREATED));

        // read interpolated response
        given().when()
                .get("/example-inline")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("Inline bar"))
                // content type inferred from response file name
                .contentType(ContentType.TEXT);
    }

    /**
     * Interpolate a request-scoped template placeholder.
     */
    @Test
    public void testRequestScopedInterpolatedTemplate() {
        given().when()
                .contentType(ContentType.JSON)
                .pathParam("petId", 99)
                .put("/pets/{petId}")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("Pet ID: 99"))
                // content type inferred from response file name
                .contentType(ContentType.TEXT);
    }

    /**
     * Interpolate a JsonPath template placeholder using a store value.
     */
    @Test
    public void testJsonPathInterpolatedTemplate() throws Exception {
        final String user = FileUtils.readFileToString(
                new File(CaptureTest.class.getResource("/response-template/user.json").toURI()),
                StandardCharsets.UTF_8
        );

        given().when()
                .body(user)
                .contentType(ContentType.JSON)
                .post("/users")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK))
                .body(equalTo("Postcode: PO5 7CO"))
                // content type inferred from response file name
                .contentType(ContentType.TEXT);
    }
}
