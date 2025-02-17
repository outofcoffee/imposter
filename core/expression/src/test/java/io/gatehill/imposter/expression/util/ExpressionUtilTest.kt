package io.gatehill.imposter.expression.util

import io.gatehill.imposter.expression.eval.ExpressionEvaluator
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

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
            onUnsupported = ExpressionUtil.UnsupportedBehaviour.NULLIFY,
        )

        assertThat(result, equalTo("fallback"))
    }

    @Test
    fun `eval invalid expression`() {
        val result = ExpressionUtil.eval(
            input = "\${invalid}",
            evaluators = emptyMap(),
            onUnsupported = ExpressionUtil.UnsupportedBehaviour.NULLIFY,
        )
        assertThat(result, equalTo(""))
    }

    @Test
    fun `ignore invalid expression`() {
        val result = ExpressionUtil.eval(
            input = "\${some.expression.with:\$inlineDollar}",
            evaluators = emptyMap(),
            onUnsupported = ExpressionUtil.UnsupportedBehaviour.IGNORE,
        )
        assertThat(result, equalTo("\${some.expression.with:\$inlineDollar}"))
    }

    @Test
    fun `ignore multiple invalid expressions`() {
        val result = ExpressionUtil.eval(
            input = "\${some.expression.with:\$inlineDollar} and \${another.expression}",
            evaluators = emptyMap(),
            onUnsupported = ExpressionUtil.UnsupportedBehaviour.IGNORE,
        )
        assertThat(result, equalTo("\${some.expression.with:\$inlineDollar} and \${another.expression}"))
    }
}
