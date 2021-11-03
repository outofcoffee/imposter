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
import io.gatehill.imposter.plugin.test.TestPluginImpl;
import io.gatehill.imposter.util.HttpUtil;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

/**
 * Tests for performance simulation.
 *
 * @author Pete Cornish
 */
@RunWith(VertxUnitRunner.class)
public class PerformanceSimulationTest extends BaseVerticleTest {
    /**
     * Tolerate (very) slow test execution conditions.
     */
    private static final int MEASUREMENT_TOLERANCE = 2000;

    @Override
    protected Class<? extends Plugin> getPluginClass() {
        return TestPluginImpl.class;
    }

    @Before
    public void setUp(TestContext testContext) throws Exception {
        super.setUp(testContext);
        RestAssured.baseURI = "http://" + getHost() + ":" + getListenPort();
    }

    @Override
    protected List<String> getTestConfigDirs() {
        return Lists.newArrayList(
                "/performance-simulation"
        );
    }

    /**
     * The response should have a latency of at least 500ms.
     */
    @Test
    public void testRequestDelayed_StaticExact() {
        final long startMs = System.currentTimeMillis();

        given().when()
                .get("/static-exact-delay")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        final long latency = System.currentTimeMillis() - startMs;
        assertTrue(
                "Response latency should be >= 500ms - was: " + latency,
                latency >= 500
        );
    }

    /**
     * The response should have a latency of roughly between 200ms-400ms,
     * plus the {@link #MEASUREMENT_TOLERANCE}.
     */
    @Test
    public void testRequestDelayed_StaticRange() {
        final long startMs = System.currentTimeMillis();

        given().when()
                .get("/static-range-delay")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        final long latency = System.currentTimeMillis() - startMs;
        assertTrue(
                "Response latency should be >= 200ms and <= 400ms - was: " + latency,
                latency >= 200 && latency <= (400 + MEASUREMENT_TOLERANCE)
        );
    }

    /**
     * The response should have a latency of at least 500ms.
     */
    @Test
    public void testRequestDelayed_ScriptedExact() {
        final long startMs = System.currentTimeMillis();

        given().when()
                .get("/scripted-exact-delay")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        final long latency = System.currentTimeMillis() - startMs;
        assertTrue(
                "Response latency should be >= 500ms - was: " + latency,
                latency >= 500
        );
    }

    /**
     * The response should have a latency of roughly between 200ms-400ms,
     * plus the {@link #MEASUREMENT_TOLERANCE}.
     */
    @Test
    public void testRequestDelayed_ScriptedRange() {
        final long startMs = System.currentTimeMillis();

        given().when()
                .get("/scripted-range-delay")
                .then()
                .statusCode(equalTo(HttpUtil.HTTP_OK));

        final long latency = System.currentTimeMillis() - startMs;
        assertTrue(
                "Response latency should be >= 200ms and <= 400ms - was: " + latency,
                latency >= 200 && latency <= (400 + MEASUREMENT_TOLERANCE)
        );
    }
}
