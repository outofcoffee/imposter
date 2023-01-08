package io.gatehill.imposter.expression.util

import io.gatehill.imposter.expression.eval.ExpressionEvaluator
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ExpressionUtilTest {
    @Test
    fun `eval missing with fallback`() {
        val result = ExpressionUtil.eval(
            input = "\${foo:-fallback}",
            evaluators = mapOf(
                "*" to object : ExpressionEvaluator<String> {
                    override val name = "foo"
                    override fun eval(expression: String, context: Map<String, *>) = null
                }
            ),
            nullifyUnsupported = true,
        )

        assertThat(result, equalTo("fallback"))
    }

    @Test
    fun `eval invalid expression`() {
        val result = ExpressionUtil.eval(
            input = "\${invalid}",
            evaluators = emptyMap(),
            nullifyUnsupported = true,
        )
        assertThat(result, equalTo(""))
    }
}
