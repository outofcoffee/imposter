/*
 * Copyright (c) 2022.
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

package io.gatehill.imposter.http

import java.util.regex.Pattern

/**
 * @author Pete Cornish
 */
data class HttpRoute(
    val path: String? = null,
    val regex: String? = null,
    val method: HttpMethod? = null
) {
    var handler: HttpExchangeHandler? = null

    private data class ParsedPathParams(
        val paramNames: List<String>,
        val pathPattern: Pattern
    )

    private val parsedPathParams by lazy {
        val paramNames = mutableListOf<String>()

        val matcher = placeholderPattern.matcher(path!!)
        val sb = StringBuffer()
        while (matcher.find()) {
            val paramName: String = matcher.group().substring(1)
            require(!paramNames.contains(paramName)) { "Cannot use param name '$paramName' more than once in path" }

            matcher.appendReplacement(sb, "(?<$paramName>[^/]+)")
            paramNames.add(paramName)
        }
        matcher.appendTail(sb)

        return@lazy ParsedPathParams(paramNames, Pattern.compile(sb.toString()))
    }

    val regexPattern: Pattern by lazy { Pattern.compile(regex!!) }

    fun handler(requestHandler: HttpExchangeHandler): HttpRoute {
        handler = requestHandler
        return this
    }

    fun isCatchAll(): Boolean = (null == path && null == regex)

    fun matches(requestPath: String): Boolean {
        return path?.let {
            requestPath == path || isPathPlaceholderMatch(requestPath)
        } ?: regex?.let {
            regexPattern.matcher(requestPath).matches()
        } ?: false
    }

    private fun isPathPlaceholderMatch(requestPath: String): Boolean {
        if (path?.contains(':') != true) {
            // no placeholders
            return false
        }
        return parsedPathParams.pathPattern.matcher(requestPath).matches()
    }

    fun extractPathParams(requestPath: String): Map<String, String> {
        val matcher = parsedPathParams.pathPattern.matcher(requestPath)
        if (matcher.matches()) {
            return parsedPathParams.paramNames.associateWith { matcher.group(it) }
        }
        return emptyMap()
    }

    companion object {
        private val placeholderPattern = Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)")
    }
}
