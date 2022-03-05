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

import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.util.BodyQueryUtil
import io.gatehill.imposter.util.DateTimeUtil
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
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
    override fun eval(expression: String, httpExchange: HttpExchange): String {
        val matcher = expressionPattern.matcher(expression)
        var matched = false
        val sb = StringBuffer()
        while (matcher.find()) {
            matched = true
            val subExpression = matcher.group(1)
            val result = evalSingle(subExpression, httpExchange) ?: ""
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
    ): String? = loadAndQuery(expression) { itemKey ->
        evalSingleInternal(itemKey, httpExchange)
    }

    /**
     * @see evalSingle
     */
    private fun evalSingleInternal(expression: String, httpExchange: HttpExchange): String? {
        return if (expression.startsWith("context.")) {
            evalContext(expression, httpExchange)
        } else if (expression.startsWith("datetime.")) {
            evalDatetime(expression)
        } else {
            LOGGER.warn("Unsupported expression: $expression")
            null
        }
    }

    /**
     * Evaluates a context expression in the form:
     * ```
     * context.a.b.c
     * ```
     */
    private fun evalContext(expression: String, httpExchange: HttpExchange): String? {
        try {
            val parts = expression.split(
                delimiters = arrayOf("."),
                ignoreCase = false,
                limit = 4,
            )
            val parsed: String? = when (parts[0]) {
                "context" -> when (parts[1]) {
                    "request" -> {
                        when (parts[2]) {
                            "body" -> checkExpression(expression, 3, parts) {
                                httpExchange.bodyAsString
                            }
                            "headers" -> checkExpression(expression, 4, parts) {
                                httpExchange.request().getHeader(parts[3])
                            }
                            "pathParams" -> checkExpression(expression, 4, parts) {
                                httpExchange.pathParam(parts[3])
                            }
                            "queryParams" -> checkExpression(expression, 4, parts) {
                                httpExchange.queryParam(parts[3])
                            }
                            else -> {
                                LOGGER.warn("Could not parse request context expression: $expression")
                                null
                            }
                        }
                    }
                    "response" -> {
                        check(httpExchange.phase == ExchangePhase.RESPONSE_SENT) {
                            "Cannot capture response outside of ${ExchangePhase.RESPONSE_SENT} phase"
                        }
                        when (parts[2]) {
                            "body" -> checkExpression(expression, 3, parts) {
                                httpExchange.response().bodyBuffer?.toString(Charsets.UTF_8)
                            }
                            "headers" -> checkExpression(expression, 4, parts) {
                                httpExchange.response().headers()[parts[3]]
                            }
                            else -> {
                                LOGGER.warn("Could not parse response context expression: $expression")
                                null
                            }
                        }
                    }
                    else -> {
                        LOGGER.warn("Could not parse context expression: $expression")
                        null
                    }
                }
                else -> {
                    LOGGER.warn("Could not parse context expression: $expression")
                    null
                }
            }
            return parsed

        } catch (e: Exception) {
            throw RuntimeException("Error evaluating context expression: $expression", e)
        }
    }

    private fun checkExpression(
        expression: String,
        minParts: Int,
        parts: List<String>,
        valueSupplier: () -> String?,
    ): String? {
        if (parts.size < minParts) {
            LOGGER.warn("Could not parse context expression: $expression - expected $minParts parts, but was ${parts.size}")
            return null
        }
        return valueSupplier()
    }

    /**
     * Evaluates a datetime expression in the form:
     * ```
     * datetime.a.b
     * ```
     */
    private fun evalDatetime(expression: String): String? {
        try {
            val parts = expression.split(
                delimiters = arrayOf("."),
                ignoreCase = false,
                limit = 3,
            )
            if (parts.size < 3) {
                LOGGER.warn("Could not parse datetime expression: $expression")
                return null
            }
            val parsed = when (parts[0]) {
                "datetime" -> when (parts[1]) {
                    "now" -> when (parts[2]) {
                        "millis" -> System.currentTimeMillis().toString()
                        "nanos" -> System.nanoTime().toString()
                        "iso8601_date" -> DateTimeUtil.DATE_FORMATTER.format(
                            LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
                        )
                        "iso8601_datetime" -> DateTimeUtil.DATE_TIME_FORMATTER.format(
                            OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                        )
                        else -> {
                            LOGGER.warn("Could not parse datetime expression: $expression")
                            null
                        }
                    }
                    else -> {
                        LOGGER.warn("Could not parse datetime expression: $expression")
                        null
                    }
                }
                else -> {
                    LOGGER.warn("Could not parse datetime expression: $expression")
                    null
                }
            }
            return parsed

        } catch (e: Exception) {
            throw RuntimeException("Error evaluating datetime expression: $expression", e)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun <T : Any> loadAndQuery(rawItemKey: String, valueResolver: (key: String) -> T?): T? {
        val itemKey: String
        val jsonPath: String?

        // check for jsonpath expression
        val colonIndex = rawItemKey.indexOf(":")
        if (colonIndex > 0) {
            jsonPath = rawItemKey.substring(colonIndex + 1)
            itemKey = rawItemKey.substring(0, colonIndex)
        } else {
            itemKey = rawItemKey
            jsonPath = null
        }

        val itemValue = valueResolver(itemKey)
        return itemValue?.let {
            jsonPath?.let { queryWithJsonPath(itemValue, jsonPath) } ?: itemValue
        }
    }

    private fun <T : Any> queryWithJsonPath(rawValue: T, jsonPath: String?): T? {
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
