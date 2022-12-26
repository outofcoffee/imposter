package io.gatehill.imposter.expression.util

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ExpressionUtilTest {
    @Test
    fun `eval invalid with fallback`() {
        val result = ExpressionUtil.eval(
            expression = "\${invalid:-fallback}",
            evaluators = emptyMap(),
        )

        assertThat(result, equalTo("fallback"))
    }

    @Test
    fun `eval invalid expression`() {
        val result = ExpressionUtil.eval(
            expression = "\${invalid}",
            evaluators = emptyMap(),
        )
        assertThat(result, equalTo(""))
    }
}
