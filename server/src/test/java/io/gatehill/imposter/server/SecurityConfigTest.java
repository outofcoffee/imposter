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
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.plugin.config.security.ConditionalNameValuePair;
import io.gatehill.imposter.plugin.config.security.SecurityCondition;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.gatehill.imposter.plugin.config.security.SecurityEffect;
import io.gatehill.imposter.plugin.test.TestPluginConfig;
import io.gatehill.imposter.plugin.test.TestPluginImpl;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.InjectorUtil;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@RunWith(VertxUnitRunner.class)
public class SecurityConfigTest extends BaseVerticleTest {
    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/security-config"
        );
    }

    @Test
    public void testPluginLoadAndConfig(TestContext testContext) {
        final PluginManager pluginManager = InjectorUtil.getInjector().getInstance(PluginManager.class);

        final TestPluginImpl plugin = pluginManager.getPlugin(TestPluginImpl.class.getCanonicalName());
        testContext.assertNotNull(plugin);

        testContext.assertNotNull(plugin.getConfigs());
        testContext.assertEquals(1, plugin.getConfigs().size());

        final TestPluginConfig pluginConfig = plugin.getConfigs().get(0);

        // check security config
        final SecurityConfig securityConfig = pluginConfig.getSecurityConfig();
        testContext.assertNotNull(securityConfig);
        testContext.assertEquals(SecurityEffect.Deny, securityConfig.getDefaultEffect());

        // check conditions
        testContext.assertEquals(2, securityConfig.getConditions().size());

        // check short configuration option
        final SecurityCondition condition1 = securityConfig.getConditions().get(0);
        testContext.assertEquals(SecurityEffect.Permit, condition1.getEffect());
        final Map<String, ConditionalNameValuePair> parsedHeaders1 = condition1.getRequestHeaders();
        testContext.assertEquals(1, parsedHeaders1.size());
        testContext.assertEquals("s3cr3t", parsedHeaders1.get("Authorization").getValue());

        // check long configuration option
        final SecurityCondition condition2 = securityConfig.getConditions().get(1);
        testContext.assertEquals(SecurityEffect.Deny, condition2.getEffect());
        final Map<String, ConditionalNameValuePair> parsedHeaders2 = condition2.getRequestHeaders();
        testContext.assertEquals(1, parsedHeaders2.size());
        testContext.assertEquals("opensesame", parsedHeaders2.get("X-Api-Key").getValue());
    }

    /**
     * Deny - no authentication provided.
     */
    @Test
    public void testRequestDenied_NoAuth() {
        given().when()
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));
    }

    /**
     * Deny - the 'Permit' condition does not match.
     */
    @Test
    public void testRequestDenied_NoPermitMatch() {
        given().when()
                .header("Authorization", "invalid-value")
                .header("X-Api-Key", "opensesame")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));
    }

    /**
     * Deny - the 'Deny' condition matches.
     */
    @Test
    public void testRequestDenied_DenyMatch() {
        given().when()
                .header("Authorization", "s3cr3t")
                .header("X-Api-Key", "does-not-match")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));
    }

    /**
     * Deny - only one condition satisfied.
     */
    @Test
    public void testRequestDenied_OnlyOneMatch() {
        given().when()
                .header("Authorization", "s3cr3t")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));

        given().when()
                .header("X-Api-Key", "opensesame")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_UNAUTHORIZED));
    }

    /**
     * Permit - both conditions are satisfied.
     */
    @Test
    public void testResourceRequestPermitted() {
        given().when()
                .header("Authorization", "s3cr3t")
                .header("X-Api-Key", "opensesame")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }

    /**
     * Permit - both conditions are satisfied, even though the case of the header
     * name differs from that in the configuration.
     */
    @Test
    public void testResourceRequestPermitted_CaseInsensitive() {
        given().when()
                .header("authorization", "s3cr3t")
                .header("x-api-key", "opensesame")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        given().when()
                .header("AUTHORIZATION", "s3cr3t")
                .header("X-API-KEY", "opensesame")
                .get("/example")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }

    /**
     * Permit - status endpoint is explicitly permitted.
     */
    @Test
    public void testStatusRequestPermitted() {
        given().when()
                .get("/system/status")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));
    }
}
