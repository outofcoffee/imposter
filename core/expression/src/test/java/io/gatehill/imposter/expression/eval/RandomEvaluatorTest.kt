/*
 * Copyright (c) 2024.
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

    @Test
    fun `generates from list of chars`() {
        val result = RandomEvaluator.eval("""random.any(chars="abc", length=5)""", emptyMap<String, Any>())
        assertThat(result, hasLength(5))
        assertContainsOnly(result, listOf('a', 'b', 'c'))
    }

    @Test
    fun `returns null for invalid chars string`() {
        val result = RandomEvaluator.eval("""random.any(chars=notquoted, length=5)""", emptyMap<String, Any>())
        Assert.assertNull(result)
    }

    @Test
    fun `returns null for invalid random type`() {
        val result = RandomEvaluator.eval("random.invalid()", emptyMap<String, Any>())
        Assert.assertNull(result)
    }

    private fun assertContainsOnly(
        actual: String?,
        allowed: List<Char>
    ) = actual?.let {
        if (!actual.all { allowed.contains(it) }) Assert.fail("Expected value to contain only $allowed but was: $actual")
    } ?: Assert.fail("Expected value to contain only $allowed but was null")
}
