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

package io.gatehill.imposter.plugin.openapi;

import com.google.common.collect.Lists;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.gatehill.imposter.server.BaseVerticleTest;
import io.gatehill.imposter.util.HttpUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static com.jayway.restassured.RestAssured.given;

/**
 * Tests for {@link OpenApiPluginImpl}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiPluginImplTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return OpenApiPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/openapi2/simple",
                "/openapi3/simple"
        );
    }

    private void assertBody(TestContext testContext, String body) {
        testContext.assertNotNull(body);

        final JsonObject jsonBody = new JsonObject(body);
        final JsonArray versions = jsonBody.getJsonArray("versions");
        testContext.assertNotNull(versions, "Versions array should exist");
        testContext.assertEquals(2, versions.size());

        // verify entries
        testContext.assertNotNull(versions.getJsonObject(0), "Version array entry 0 should exist");
        testContext.assertNotNull(versions.getJsonObject(1), "Version array entry 1 should exist");
    }

    /**
     * Should return the example from the specification for the default HTTP 200 status code, since the
     * content type in the 'Accept' header matches that in the specification example.
     *
     * @param testContext
     */
    @Test
    public void testServeDefaultExampleMatchContentType(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                // JSON content type in 'Accept' header matches specification example
                .accept(ContentType.JSON)
                .when()
                .get("/simple/apis")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        assertBody(testContext, body);
    }

    /**
     * Should return the example from the specification for the default HTTP 200 status code, even though the
     * content type in the 'Accept' header does not match that in the specification example.
     *
     * @param testContext
     * @see OpenApiPluginConfig#isPickFirstIfNoneMatch()
     */
    @Test
    public void testServeDefaultExampleNoExactMatch(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                // do not set JSON content type in 'Accept' header, to force mismatch against specification example
                .accept(ContentType.TEXT)
                .when()
                .get("/simple/apis")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        assertBody(testContext, body);
    }

    /**
     * Should return the specification UI.
     *
     * @param testContext
     */
    @Test
    public void testGetSpecUi(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                .accept(ContentType.TEXT)
                .when()
                .get(OpenApiPluginImpl.SPECIFICATION_PATH + "/")
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        testContext.assertTrue(body.contains("</html>"));
    }

    /**
     * Should return a combined specification.
     *
     * @param testContext
     */
    @Test
    public void testGetCombinedSpec(TestContext testContext) {
        final String body = given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get(OpenApiPluginImpl.COMBINED_SPECIFICATION_PATH)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        testContext.assertNotNull(body);

        final SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(body, Collections.emptyList(), new ParseOptions());
        testContext.assertNotNull(parseResult);

        final OpenAPI combined = parseResult.getOpenAPI();
        testContext.assertNotNull(combined);

        testContext.assertNotNull(combined.getInfo());
        testContext.assertEquals("Imposter Mock APIs", combined.getInfo().getTitle());

        // should contain combination of all specs' endpoints
        testContext.assertEquals(6, combined.getPaths().size());

        // OASv2
        testContext.assertTrue(combined.getPaths().containsKey("/apis"));
        testContext.assertTrue(combined.getPaths().containsKey("/v2"));
        testContext.assertTrue(combined.getPaths().containsKey("/pets"));
        testContext.assertTrue(combined.getPaths().containsKey("/pets/{id}"));

        // OASv3
        testContext.assertTrue(combined.getPaths().containsKey("/oas3/apis"));
        testContext.assertTrue(combined.getPaths().containsKey("/oas3/v2"));
    }

    /**
     * Should return examples formatted as JSON.
     *
     * @param testContext
     */
    @Test
    public void testExamples(TestContext testContext) {
        // OASv2
        queryEndpoint("/simple/apis", responseBody -> {
            final String trimmed = responseBody.trim();
            testContext.assertTrue(trimmed.startsWith("{"));
            testContext.assertTrue(trimmed.contains("CURRENT"));
            testContext.assertTrue(trimmed.endsWith("}"));
        });

        queryEndpoint("/api/pets/1", responseBody -> {
            final String trimmed = responseBody.trim();
            testContext.assertTrue(trimmed.startsWith("{"));
            testContext.assertTrue(trimmed.contains("Fluffy"));
            testContext.assertTrue(trimmed.endsWith("}"));
        });

        // OASv3
        queryEndpoint("/oas3/apis", responseBody -> {
            final String trimmed = responseBody.trim();
            testContext.assertTrue(trimmed.startsWith("{"));
            testContext.assertTrue(trimmed.contains("CURRENT"));
            testContext.assertTrue(trimmed.endsWith("}"));
        });
    }

    private void queryEndpoint(String url, Consumer<String> bodyConsumer) {
        final String body = given()
                .log().ifValidationFails()
                .accept(ContentType.JSON)
                .when()
                .get(url)
                .then()
                .log().ifValidationFails()
                .statusCode(HttpUtil.HTTP_OK)
                .extract().asString();

        bodyConsumer.accept(body);
    }
}
