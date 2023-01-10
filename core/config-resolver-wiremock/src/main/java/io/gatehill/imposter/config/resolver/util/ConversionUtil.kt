/*
 * Copyright (c) 2023.
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
package io.gatehill.imposter.config.resolver.util

import io.gatehill.imposter.config.resolver.model.BodyPattern
import io.gatehill.imposter.plugin.config.resource.ResourceMatchOperator
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyConfig
import io.gatehill.imposter.script.FailureSimulationType
import java.util.regex.Pattern

object ConversionUtil {
    /**
     * Examples:
     *
     *     {{jsonPath request.body '$.foo'}}
     *     {{xPath request.body '//foo'}}
     */
    private val placeholderPattern = Pattern.compile("\\{\\{\\s?(.+?)\\s+(.+?)\\s?}}")

    private val whitespacePattern = Pattern.compile("\\s+")

    private val expressionHandlers: Map<String, ExpressionHandler> = mapOf(
        "jsonPath" to convertQuery { input, expression -> "\\\${${input}:${expression}}" },
        "soapXPath" to convertQuery { input, expression -> "\\\${${input}:!/*[local-name()='Envelope']/*[local-name()='Body']${expression}}" },
        "xPath" to convertQuery { input, expression -> "\\\${${input}:${expression}}" },
        "randomValue" to convertRandom()
    )

    fun convertBodyPatterns(bodyPatterns: List<BodyPattern>?): RequestBodyConfig? {
        if (bodyPatterns?.isNotEmpty() == true) {
            if (bodyPatterns.size == 1) {
                return convertBodyPattern(bodyPatterns.first())
            } else {
                return RequestBodyConfig().apply { allOf = bodyPatterns.map { convertBodyPattern(it) } }
            }
        } else {
            return null
        }
    }

    fun convertQueryParams(query: String?) =
        query?.let { query.split('&').map { it.split('=') }.map { it[0] to it[1] } }?.toMap()

    fun convertHeaders(headers: Map<String, Map<String, String>>?) =
        headers?.mapNotNull { (k, v) -> v["equalTo"]?.let { k to it } }?.toMap()

    fun convertFault(fault: String?) = when (fault) {
        "EMPTY_RESPONSE" -> FailureSimulationType.EmptyResponse
        // not like-for-like behaviour
        "CONNECTION_RESET_BY_PEER" -> FailureSimulationType.CloseConnection
        else -> null
    }

    /**
     * Convert a body pattern to its corresponding request body configuration. Example patterns shown below.
     *
     * Contains:
     * ```
     * {
     *   "matchesXPath": {
     *     "expression": "//foo/text()",
     *     "contains": "bar"
     *    }
     * }
     * ```
     *
     * Equality:
     * ```
     * {
     *   "matchesXPath": {
     *     "expression": "//foo/text()",
     *     "equalTo": "baz"
     *    }
     * }
     * ```
     *
     * Regex:
     * ```
     * {
     *   "matchesXPath": {
     *     "expression": "//foo/text()",
     *     "matches": "f.*"
     *    }
     * }
     * ```
     *
     * String form:
     * ```
     * {
     *   "matchesXPath": "/qux:corge",
     *   "xPathNamespaces": {
     *     "qux" : "http://example.com/somens"
     *   }
     * }
     * ```
     */
    private fun convertBodyPattern(bodyPattern: BodyPattern): RequestBodyConfig {
        val requestBodyConfig = RequestBodyConfig()
        when (val matchesXPath = bodyPattern.matchesXPath) {
            is String -> {
                // body pattern using XPath with embedded conditional check, but no value;
                // treat as existence check
                requestBodyConfig.xPath = matchesXPath
                requestBodyConfig.operator = ResourceMatchOperator.Exists
            }
            is Map<*, *> -> {
                matchesXPath["expression"]?.let { expression ->
                    requestBodyConfig.xPath = "!$expression"
                }
                // operator
                matchesXPath["equalTo"]?.let { equalTo ->
                    requestBodyConfig.value = equalTo.toString()
                } ?: matchesXPath["contains"]?.let { contains ->
                    requestBodyConfig.value = contains.toString()
                    requestBodyConfig.operator = ResourceMatchOperator.Contains
                } ?: matchesXPath["matches"]?.let { matches ->
                    requestBodyConfig.value = matches.toString()
                    requestBodyConfig.operator = ResourceMatchOperator.Matches
                } ?: matchesXPath["doesNotMatch"]?.let { matches ->
                    requestBodyConfig.value = matches.toString()
                    requestBodyConfig.operator = ResourceMatchOperator.NotMatches
                }
            }
        }
        requestBodyConfig.xmlNamespaces = bodyPattern.xPathNamespaces
        return requestBodyConfig
    }

    fun convertPlaceholders(source: String): String {
        val matcher = placeholderPattern.matcher(source)
        var matched = false
        val sb = StringBuffer()
        while (matcher.find()) {
            matched = true
            val args = matcher.group(2).split(whitespacePattern)
            val handler = expressionHandlers[matcher.group(1)]
            val result = handler?.invoke(args) ?: "null"
            matcher.appendReplacement(sb, result)
        }
        return if (matched) {
            matcher.appendTail(sb)
            sb.toString()
        } else {
            source
        }
    }

    /**
     * Example:
     *
     *      xPath request.body '//foo/bar'
     */
    private fun convertQuery(handler: (input: String, expression: String) -> String): ExpressionHandler = { args ->
        val input = args[0].let { if (it.startsWith("request.")) "context.$it" else it }
        val expression = args[1].removeSurrounding("'")
        handler(input, expression)
    }

    /**
     * Example:
     *
     *     randomValue length=6 type='ALPHABETIC' uppercase=true
     */
    private fun convertRandom(): ExpressionHandler = { args ->
        var length: Int? = null
        var type: String? = null
        var uppercase = false
        for (arg in args) {
            if (arg.startsWith("length=")) {
                length = arg.substringAfter("=").toInt()
            } else if (arg.startsWith("type=")) {
                type = arg.substringAfter("=").removeSurrounding("'").lowercase()
            } else if (arg.startsWith("uppercase=")) {
                uppercase = arg.substringAfter("=").toBoolean()
            }
        }
        checkNotNull(type) { "type is required" }
        checkNotNull(length) { "length is required" }
        "\\\${random.$type(length=$length,uppercase=$uppercase)}"
    }
}

private typealias ExpressionHandler = (args: List<String>) -> String
