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
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
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
        RestAssured.baseURI = "http://" + HOST + ":" + getListenPort();
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
