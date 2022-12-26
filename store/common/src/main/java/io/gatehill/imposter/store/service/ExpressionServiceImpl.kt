/*
 * Copyright (c) 2016-2021.
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
package io.gatehill.imposter.store.service

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.store.service.expression.ExpressionEvaluator
import io.gatehill.imposter.util.BodyQueryUtil
import org.apache.logging.log4j.LogManager
import java.util.regex.Pattern

/**
 * Evaluates expressions against the [HttpExchange].
 *
 * @author Pete Cornish
 */
class ExpressionServiceImpl : ExpressionService {
    /**
     * {@inheritDoc}
     */
    override fun eval(expression: String, httpExchange: HttpExchange, evaluators: Map<String, ExpressionEvaluator<*>>?): String {
        val finalEvaluators = evaluators ?: ExpressionService.builtin
        val matcher = expressionPattern.matcher(expression)
        var matched = false
        val sb = StringBuffer()
        while (matcher.find()) {
            matched = true
            val subExpression = matcher.group(1)
            val result = evalSingle(subExpression, httpExchange, finalEvaluators) ?: ""
            matcher.appendReplacement(sb, result)
        }
        return if (matched) {
            matcher.appendTail(sb)
            sb.toString()
        } else {
            expression
        }
    }

    /**
     * Evaluates a single expression in the form `expression`
     * or `expression:$.jp`, where `$.jp` is a valid JsonPath expression.
     *
     * Note: [expression] does not have the template syntax surrounding it.
     */
    private fun evalSingle(
        expression: String,
        httpExchange: HttpExchange,
        evaluators: Map<String, ExpressionEvaluator<*>>
    ): String? = loadAndQuery(expression) { itemKey ->
        evalSingleInternal(itemKey, httpExchange, evaluators)
    }

    /**
     * @see evalSingle
     */
    private fun evalSingleInternal(
        expression: String,
        httpExchange: HttpExchange,
        evaluators: Map<String, ExpressionEvaluator<*>>,
    ): Any? {
        val root = expression.substringBefore(".").takeIf { it.isNotEmpty() }
        LOGGER.trace("Evaluating expression: {}", expression)

        // fallback to wildcard evaluator if no explicit match
        val evaluator = evaluators[root] ?: evaluators["*"]
        evaluator?.let {
            LOGGER.trace("Using {} expression evaluator for expression: {}", evaluator.name, expression)
            return evaluator.eval(expression, httpExchange) ?: run {
                LOGGER.debug("Expression: {} evaluated to null", expression)
                null
            }
        }

        LOGGER.warn("Unsupported expression: $expression")
        return null
    }

    /**
     * Loads a value for the specified key, optionally applying a JsonPath query
     * to the value.
     *
     * The [rawItemKey] can be in the form of a string such as `a.b.c`, or, optionally
     * include a JsonPath query, prefixed with a colon, such as `a.b.c:$.jp`, where
     * `$.jp` is a valid JsonPath expression.
     *
     * @param rawItemKey the placeholder key
     * @param valueResolver the function to resolve the value, prior to any querying
     */
    private fun <T : Any> loadAndQuery(rawItemKey: String, valueResolver: (key: String) -> T?): String? {
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

        val resolvedValue = valueResolver(itemKey)?.let { itemValue ->
            jsonPath?.let { queryWithJsonPath(itemValue, jsonPath) } ?: itemValue
        }
        return resolvedValue?.toString() ?: fallbackValue
    }

    private fun <T : Any> queryWithJsonPath(rawValue: T, jsonPath: String?): T? {
        LOGGER.trace("Evaluating JSONPath: {} on expression value: {}", jsonPath, rawValue)
        val context = when (rawValue) {
            // raw JSON will be parsed by the context
            is String -> BodyQueryUtil.JSONPATH_PARSE_CONTEXT.parse(rawValue as String)

            // assumes already deserialised
            else -> BodyQueryUtil.JSONPATH_PARSE_CONTEXT.parse(rawValue)
        }
        return context.read(jsonPath)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ExpressionServiceImpl::class.java)

        /**
         * Matches instances of:
         * ```
         * ${something}
         * ```
         * ...with the group being the characters between brackets.
         */
        private val expressionPattern = Pattern.compile("\\$\\{(.+?)}")
    }
}
