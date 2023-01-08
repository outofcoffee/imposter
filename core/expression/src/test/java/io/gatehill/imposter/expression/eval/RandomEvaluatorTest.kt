package io.gatehill.imposter.expression.eval

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.text.CharSequenceLength.hasLength
import org.junit.Assert
import org.junit.Test

class RandomEvaluatorTest {
    @Test
    fun `generates random numeric`() {
        val result = RandomEvaluator.eval("random.numeric(length=2)", emptyMap<String, Any>())
        assertThat(result, hasLength(2))
        assertContainsOnly(result, RandomEvaluator.numbers.toList())
    }

    @Test
    fun `generates random alphabetic`() {
        val result = RandomEvaluator.eval("random.alphabetic(length=3)", emptyMap<String, Any>())
        assertThat(result, hasLength(3))
        assertContainsOnly(result, RandomEvaluator.alphabetUpper + RandomEvaluator.alphabetLower)
    }

    @Test
    fun `generates random alphanumeric`() {
        val result = RandomEvaluator.eval("random.alphanumeric(length=4)", emptyMap<String, Any>())
        assertThat(result, hasLength(4))
        assertContainsOnly(
            result,
            RandomEvaluator.alphabetUpper + RandomEvaluator.alphabetLower + RandomEvaluator.numbers
        )
    }

    @Test
    fun `generates uuid`() {
        val result = RandomEvaluator.eval("random.uuid()", emptyMap<String, Any>())
        assertThat(result, hasLength(36))
        assertContainsOnly(result, RandomEvaluator.alphabetLower + RandomEvaluator.numbers + listOf('-'))
    }

    private fun assertContainsOnly(
        actual: String?,
        allowed: List<Char>
    ) = actual?.let {
        if (!actual.all { allowed.contains(it) }) Assert.fail("Expected value to contain only $allowed but was: $actual")
    } ?: Assert.fail("Expected value to contain only $allowed but was null")
}
