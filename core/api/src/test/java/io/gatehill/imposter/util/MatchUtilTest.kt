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

package io.gatehill.imposter.util

import io.gatehill.imposter.plugin.config.resource.conditional.ConditionalNameValuePair
import io.gatehill.imposter.plugin.config.resource.conditional.MatchOperator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Tests for [MatchUtil].
 */
class MatchUtilTest {
    @Test
    fun `match using equality operator`() {
        assertTrue(MatchUtil.conditionMatches("expected", MatchOperator.EqualTo, "expected"))
        assertFalse(MatchUtil.conditionMatches("expected", MatchOperator.EqualTo, "unexpected"))
    }

    @Test
    fun `inverse match using equality operator`() {
        assertTrue(MatchUtil.conditionMatches("expected", MatchOperator.NotEqualTo, "unexpected"))
        assertFalse(MatchUtil.conditionMatches("expected", MatchOperator.NotEqualTo, "expected"))
    }

    @Test
    fun `match using contains operator`() {
        assertTrue(MatchUtil.conditionMatches("lorem", MatchOperator.Contains, "loremipsum"))
        assertFalse(MatchUtil.conditionMatches("dolor", MatchOperator.Contains, "loremipsum"))
    }

    @Test
    fun `inverse match using contains operator`() {
        assertTrue(MatchUtil.conditionMatches("dolor", MatchOperator.NotContains, "loremipsum"))
        assertFalse(MatchUtil.conditionMatches("lorem", MatchOperator.NotContains, "loremipsum"))
    }

    @Test
    fun `match using matches operator`() {
        assertTrue(MatchUtil.conditionMatches("[a-z]+", MatchOperator.Matches, "alpha"))
        assertFalse(MatchUtil.conditionMatches("[0-9]+", MatchOperator.Matches, "alpha"))
    }

    @Test
    fun `inverse match using matches operator`() {
        assertTrue(MatchUtil.conditionMatches("[0-9]+", MatchOperator.NotMatches, "alpha"))
        assertFalse(MatchUtil.conditionMatches("[a-z]+", MatchOperator.NotMatches, "alpha"))
    }

    @Test
    fun `match using exists operator`() {
        assertTrue(MatchUtil.conditionMatches(null, MatchOperator.Exists, "expected"))
        assertFalse(MatchUtil.conditionMatches(null, MatchOperator.Exists, null))
    }

    @Test
    fun `inverse match using exists operator`() {
        assertTrue(MatchUtil.conditionMatches(null, MatchOperator.NotExists, null))
        assertFalse(MatchUtil.conditionMatches(null, MatchOperator.NotExists, "expected"))
    }

    @Test
    fun `match using conditional name value pair`() {
        assertTrue(MatchUtil.conditionMatches(ConditionalNameValuePair("param", "expected", MatchOperator.EqualTo), "expected"))
        assertFalse(MatchUtil.conditionMatches(ConditionalNameValuePair("param", "expected", MatchOperator.EqualTo), "unexpected"))
    }

    @Test
    fun `safe equals`() {
        assertTrue(MatchUtil.safeEquals("expected", "expected"))
        assertFalse(MatchUtil.safeEquals("expected", "unexpected"))
        assertTrue(MatchUtil.safeEquals(null, null))
        assertFalse(MatchUtil.safeEquals(null, "foo"))
        assertFalse(MatchUtil.safeEquals("foo", null))
    }

    @Test
    fun `safe contains`() {
        assertTrue(MatchUtil.safeContains("loremipsum", "lorem"))
        assertFalse(MatchUtil.safeContains("loremipsum", "dolor"))
        assertFalse(MatchUtil.safeContains(null, "lorem"))
        assertFalse(MatchUtil.safeContains("loremipsum", null))
    }
}
