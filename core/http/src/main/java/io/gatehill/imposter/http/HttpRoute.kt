/*
 * Copyright (c) 2022-2024.
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
    val router: HttpRouter,
    val path: String? = null,
    val regex: String? = null,
    val method: HttpMethod? = null,
) {
    val hasTrailingWildcard = path?.endsWith('*') ?: false
    var handler: HttpExchangeFutureHandler? = null

    private data class ParsedPathParams(
        val paramNames: List<String>,
        val pathPattern: Pattern
    )

    private val parsedPathParams: ParsedPathParams? by lazy {
        path ?: return@lazy null

        val paramNames = mutableListOf<String>()
        val matcher = PATH_PARAM_PLACEHOLDER.matcher(path)
        val sb = StringBuffer()
        while (matcher.find()) {
            val paramName: String = matcher.group(1)
            require(!paramNames.contains(paramName)) { "Cannot use param name '$paramName' more than once in path" }

            matcher.appendReplacement(sb, "(?<$paramName>[^/]+)")
            paramNames.add(paramName)
        }
        matcher.appendTail(sb)

        return@lazy ParsedPathParams(paramNames, Pattern.compile(sb.toString()))
    }

    private val regexPattern: Pattern by lazy { Pattern.compile(regex!!) }

    fun handler(requestHandler: HttpExchangeFutureHandler): HttpRoute {
        handler = requestHandler
        return this
    }

    fun isCatchAll(): Boolean = (null == path && null == regex)

    fun matches(requestPath: String): Boolean {
        return path?.let {
            requestPath == path || isPathPlaceholderMatch(requestPath) || isTrailingWildcardMatch(requestPath)
        } ?: regex?.let {
            regexPattern.matcher(requestPath).matches()
        } ?: false
    }

    private fun isPathPlaceholderMatch(requestPath: String): Boolean {
        parsedPathParams?.let { pathParams ->
            if (path?.contains('{') == true) {
                val matcher = pathParams.pathPattern.matcher(requestPath)
                if (matcher.matches()) {
                    return true
                }
            }
        }
        return false
    }

    private fun isTrailingWildcardMatch(requestPath: String): Boolean {
        if (hasTrailingWildcard) {
            path?.let {
                return requestPath.startsWith(path.substring(0, path.length - 1))
            }
        }
        // no wildcard
        return false
    }

    fun extractPathParams(requestPath: String): Map<String, String> {
        parsedPathParams?.let { pathParams ->
            val matcher = pathParams.pathPattern.matcher(requestPath)
            if (matcher.matches()) {
                return pathParams.paramNames.associateWith { matcher.group(it) }
            }
        }
        return emptyMap()
    }

    companion object {
        /**
         * Path parameter placeholders are bracketed, e.g. `{paramName}`.
         */
        val PATH_PARAM_PLACEHOLDER: Pattern = Pattern.compile("\\{([a-zA-Z0-9._\\-]+)}")
    }
}
