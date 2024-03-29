/*
 * Copyright (c) 2016-2024.
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

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.gatehill.imposter.plugin.config.resource.conditional.ConditionalNameValuePair
import io.gatehill.imposter.plugin.config.resource.conditional.MatchOperator
import java.util.Objects
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author Pete Cornish
 */
object MatchUtil {
    private val patternCacheSize: Long =
        System.getenv("IMPOSTER_MATCHER_REGEX_CACHE_SIZE")?.toLong() ?: 30

    private var patternCache: Cache<String, Pattern>? = null

    /**
     * Checks if the condition is satisfied by the actual value.
     */
    fun conditionMatches(
        expected: String?,
        operator: MatchOperator,
        actual: String?,
    ): Boolean = when (operator) {
        MatchOperator.Exists -> null != actual
        MatchOperator.NotExists -> null == actual
        MatchOperator.EqualTo -> safeEquals(actual, expected)
        MatchOperator.NotEqualTo -> !safeEquals(actual, expected)
        MatchOperator.Contains -> safeContains(actual, expected)
        MatchOperator.NotContains -> !safeContains(actual, expected)
        MatchOperator.Matches -> safeRegexMatch(actual, expected)
        MatchOperator.NotMatches -> !safeRegexMatch(actual, expected)
    }

    /**
     * Checks if the condition is satisfied by the actual value.
     */
    fun conditionMatches(condition: ConditionalNameValuePair, actual: String?): Boolean =
        conditionMatches(condition.value, condition.operator, actual)

    /**
     * Checks if two objects match, where either input could be null.
     *
     * @param a object to test, possibly `null`
     * @param b object to test, possibly `null`
     * @return `true` if the objects match, otherwise `false`
     */
    fun safeEquals(a: Any?, b: Any?): Boolean {
        return if (Objects.nonNull(a)) {
            a == b
        } else {
            Objects.isNull(b)
        }
    }

    /**
     * Checks if the actual value contains the expected value.
     */
    fun safeContains(actual: String?, expected: String?) =
        if (actual != null && expected != null) {
            actual.toString().contains(expected)
        } else {
            false
        }

    /**
     * Checks if the actual value matches the given regular expression.
     */
    private fun safeRegexMatch(actualValue: String?, expression: String?): Boolean {
        return expression?.let {
            val actual = actualValue ?: ""
            if (patternCacheSize > 0) {
                getMatcher(expression, actual).matches()
            } else {
                expression.toRegex().matches(actual)
            }
        } ?: false
    }

    private fun getMatcher(expression: String, actualValue: String): Matcher {
        val cache: Cache<String, Pattern> = patternCache ?: run {
            synchronized(this) {
                // double-guard
                patternCache = patternCache ?: CacheBuilder.newBuilder()
                    .maximumSize(patternCacheSize)
                    .build()
            }
            patternCache!!
        }
        val pattern = cache.get(expression) { Pattern.compile(expression) }
        return pattern.matcher(actualValue)
    }
}
