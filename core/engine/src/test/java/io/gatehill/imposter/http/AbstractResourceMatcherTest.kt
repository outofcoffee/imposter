package io.gatehill.imposter.http

import io.gatehill.imposter.config.ResolvedResourceConfig
import io.gatehill.imposter.plugin.config.PluginConfig
import io.gatehill.imposter.plugin.config.PluginConfigImpl
import io.gatehill.imposter.util.ResourceUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

/**
 * Tests for [AbstractResourceMatcher].
 */
class AbstractResourceMatcherTest {
    private lateinit var matcher: AbstractResourceMatcher

    @BeforeEach
    fun setup() {
        matcher = object : AbstractResourceMatcher() {
            override fun matchRequest(
                pluginConfig: PluginConfig,
                resource: ResolvedResourceConfig,
                httpExchange: HttpExchange
            ) = throw NotImplementedError()
        }
    }

    @Test
    fun determineMatch() {
        val results = listOf(
            ResourceMatchResult.exactMatch("condition 1"),
            ResourceMatchResult.exactMatch("condition 2", 2),
        )
        val resource = ResolvedResourceConfig(PluginConfigImpl(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
        val outcome = determineMatch(results, resource)

        assertThat(outcome.matched, equalTo(true))
        assertThat(outcome.exact, equalTo(true))

        // the score must be the sum of the results' score
        assertThat(outcome.score, equalTo(3))

        // should be the same resource ref
        assertSame(resource, outcome.resource)
    }

    @Test
    fun `sum of weights should include wildcard and exact`() {
        val results = listOf(
            ResourceMatchResult.wildcardMatch("condition 1"),
            ResourceMatchResult.exactMatch("condition 2", 2),
        )
        val resource = ResolvedResourceConfig(PluginConfigImpl(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
        val outcome = determineMatch(results, resource)

        assertThat(outcome.matched, equalTo(true))
        assertThat(outcome.exact, equalTo(false))

        // the score must be the sum of the results' score
        assertThat(outcome.score, equalTo(3))

        // should be the same resource ref
        assertSame(resource, outcome.resource)
    }

    @Test
    fun `results should not match`() {
        val results = listOf(
            ResourceMatchResult.exactMatch("condition 1"),
            ResourceMatchResult.exactMatch("condition 2", 2),
            ResourceMatchResult.notMatched("condition 3"),
        )
        val resource = ResolvedResourceConfig(PluginConfigImpl(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
        val outcome = determineMatch(results, resource)

        assertThat(outcome.matched, equalTo(false))
    }

    @Test
    fun `no match if all have no config`() {
        val results = listOf(
            ResourceMatchResult.noConfig("condition 1"),
            ResourceMatchResult.noConfig("condition 2"),
        )
        val resource = ResolvedResourceConfig(PluginConfigImpl(), emptyMap(), emptyMap(), emptyMap(), emptyMap())
        val outcome = determineMatch(results, resource)

        assertThat(outcome.matched, equalTo(false))
    }

    private fun determineMatch(results: List<ResourceMatchResult>, resource: ResolvedResourceConfig): AbstractResourceMatcher.MatchedResource {
        val request = mock<HttpRequest> {
            on { method } doReturn HttpMethod.GET
            on { absoluteUri } doReturn "http://localhost:8080/test"
        }
        val httpExchange = mock<HttpExchange> {
            on { this.request } doReturn request
            on { this.get<String>(eq(ResourceUtil.RC_REQUEST_ID_KEY)) } doReturn "1"
        }
        return matcher.determineMatch(results, resource, httpExchange)
    }
}
