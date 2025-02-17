package io.gatehill.imposter.config.expression

import io.gatehill.imposter.ImposterConfig
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [SystemEvaluatorImpl].
 */
class SystemEvaluatorTest {
    private lateinit var systemEvaluator: SystemEvaluatorImpl

    @BeforeEach
    fun before() {
        val config = ImposterConfig().apply {
            listenPort = 8080
            serverUrl = "http://localhost:8080"
        }
        systemEvaluator = SystemEvaluatorImpl(config)
    }

    @Test
    fun eval() {
        assertThat(eval("system.server.port"), equalTo("8080"))
        assertThat(eval("system.server.url"), equalTo("http://localhost:8080"))
    }

    private fun eval(expression: String): String? =
            systemEvaluator.eval(expression, emptyMap<String, Any>())
}
