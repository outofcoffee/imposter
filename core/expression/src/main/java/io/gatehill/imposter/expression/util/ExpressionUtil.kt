/*
 * Copyright (c) 2016-2022.
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
package io.gatehill.imposter.expression.util

import io.gatehill.imposter.expression.JsonPathProvider
import io.gatehill.imposter.expression.eval.ExpressionEvaluator
import org.apache.logging.log4j.LogManager
import java.util.regex.Pattern

/**
 * Evaluates expressions against a context.
 *
 * @author Pete Cornish
 */
object ExpressionUtil {
    private val LOGGER = LogManager.getLogger(ExpressionUtil::class.java)

    /**
     * Matches instances of:
     * ```
     * ${something}
     * ```
     * ...with the group being the characters between brackets.
     */
    private val expressionPattern = Pattern.compile("\\$\\{(.+?)}")

    /**
     * Evaluates an expression in the form:
     * ```
     * ${expression}
     * ```
     * or composite expressions such as:
     * ```
     * ${expression1}...${expression2}...
     * ```
     * If no expression is found, [expression] is returned.
     */
    fun eval(
        expression: String,
        evaluators: Map<String, ExpressionEvaluator<*>>,
        context: Map<String, Any> = emptyMap(),
        jsonPathProvider: JsonPathProvider? = null,
        nullifyUnsupported: Boolean,
    ): String {
        val matcher = expressionPattern.matcher(expression)
        var matched = false
        val sb = StringBuffer()
        while (matcher.find()) {
            matched = true
            val subExpression = matcher.group(1)
            val result = evalSingle(subExpression, evaluators, context, jsonPathProvider, nullifyUnsupported)
            matcher.appendReplacement(sb, result)
            LOGGER.trace("{}={}", subExpression, result)
        }
        return if (matched) {
            matcher.appendTail(sb)
            sb.toString()
        } else {
            expression
        }
    }

    private fun evalSingle(
        subExpression: String,
        evaluators: Map<String, ExpressionEvaluator<*>>,
        context: Map<String, Any>,
        jsonPathProvider: JsonPathProvider?,
        nullifyUnsupported: Boolean,
    ): String? {
        var result: String? = null

        val evaluator = lookupEvaluator(subExpression, evaluators)
        evaluator?.let {
            result = loadAndQuery(subExpression, context, evaluator, jsonPathProvider) ?: ""

        } ?: run {
            if (nullifyUnsupported) {
                LOGGER.warn("Unsupported expression: $subExpression")
                result = ""
            } else {
                // don't replace; should ignore match
                result = "\\\${$subExpression}"
            }
        }
        return result
    }

    private fun lookupEvaluator(
        expression: String,
        evaluators: Map<String, ExpressionEvaluator<*>>,
    ): ExpressionEvaluator<*>? {
        val root = expression.substringBefore(".").takeIf { it.isNotEmpty() }
        LOGGER.trace("Evaluating expression: {}", expression)

        // fallback to wildcard evaluator if no explicit match
        val evaluator = evaluators[root] ?: evaluators["*"]
        evaluator?.also {
            LOGGER.trace("Using {} expression evaluator for expression: {}", evaluator.name, expression)
        } ?: run {
            if (LOGGER.isTraceEnabled) {
                LOGGER.trace("Unsupported expression: {}, evaluators: {}", expression, evaluators.keys)
            }
        }
        return evaluator
    }

    /**
     * Evaluates a single expression in the form `expression`
     * or `expression:$.jp`, where `$.jp` is a valid JsonPath expression.
     *
     * Note: [rawItemKey] does not have the template syntax surrounding it.
     *
     * Loads a value for the specified key, optionally applying a JsonPath query
     * to the value.
     *
     * The [rawItemKey] can be in the form of a string such as `a.b.c`, or, optionally
     * include a JsonPath query, prefixed with a colon, such as `a.b.c:$.jp`, where
     * `$.jp` is a valid JsonPath expression.
     *
     * @param rawItemKey the placeholder key
     * @param evaluator the evaluator to provide the value, prior to any querying
     */
    private fun loadAndQuery(
        rawItemKey: String,
        context: Map<String, *>,
        evaluator: ExpressionEvaluator<*>,
        jsonPathProvider: JsonPathProvider?,
    ): String? {
        val itemKey: String
        var jsonPath: String? = null
        var fallbackValue: String? = null

        // check for jsonpath expression
        val colonIndex = rawItemKey.indexOf(":")
        if (colonIndex > 0) {
            when (rawItemKey[colonIndex + 1]) {
                '$' -> jsonPath = rawItemKey.substring(colonIndex + 1)
                '-' -> fallbackValue = rawItemKey.substring(colonIndex + 2)
            }
            itemKey = rawItemKey.substring(0, colonIndex)
        } else {
            itemKey = rawItemKey
        }

        val resolvedValue = evaluator.eval(itemKey, context)?.let { itemValue ->
            jsonPath?.let { jsonPathProvider?.queryWithJsonPath(itemValue, jsonPath) } ?: itemValue
        } ?: run {
            LOGGER.debug("Expression: {} evaluated to null", itemKey)
            null
        }
        LOGGER.trace("Resolved {} to value: {}, fallback: {}", itemKey, resolvedValue, fallbackValue)
        return resolvedValue?.toString() ?: fallbackValue
    }
}
