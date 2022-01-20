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
package io.gatehill.imposter.http

import io.gatehill.imposter.plugin.config.resource.ResourceMethod
import io.vertx.core.Vertx
import java.util.regex.Pattern

/**
 * @author Pete Cornish
 */
class HttpRouter(val vertx: Vertx) {
    val routes = mutableListOf<HttpRoute>()
    val errorHandlers = mutableMapOf<Int, HttpExchangeHandler>()

    fun route(): HttpRoute {
        return HttpRoute().also(routes::add)
    }

    fun route(path: String): HttpRoute {
        return HttpRoute(path = path).also(routes::add)
    }

    fun route(method: ResourceMethod, path: String): HttpRoute {
        return HttpRoute(path = path, method = method).also(routes::add)
    }

    fun routeWithRegex(method: ResourceMethod, regex: String): HttpRoute {
        return HttpRoute(regex = regex, method = method).also(routes::add)
    }

    fun get(path: String): HttpRoute {
        return route(ResourceMethod.GET, path)
    }

    fun post(path: String): HttpRoute {
        return route(ResourceMethod.POST, path)
    }

    fun put(path: String): HttpRoute {
        return route(ResourceMethod.PUT, path)
    }

    fun patch(path: String): HttpRoute {
        return route(ResourceMethod.PATCH, path)
    }

    fun delete(path: String): HttpRoute {
        return route(ResourceMethod.DELETE, path)
    }

    fun options(path: String): HttpRoute {
        return route(ResourceMethod.OPTIONS, path)
    }

    fun head(path: String): HttpRoute {
        return route(ResourceMethod.HEAD, path)
    }

    fun connect(path: String): HttpRoute {
        return route(ResourceMethod.CONNECT, path)
    }

    fun trace(path: String): HttpRoute {
        return route(ResourceMethod.TRACE, path)
    }

    fun getWithRegex(regex: String): HttpRoute {
        return routeWithRegex(ResourceMethod.GET, regex)
    }

    fun errorHandler(statusCode: Int, handler: HttpExchangeHandler) {
        errorHandlers[statusCode] = handler
    }

    companion object {
        fun router(vertx: Vertx): HttpRouter {
            return HttpRouter(vertx)
        }
    }
}

typealias HttpExchangeHandler = (HttpExchange) -> Unit

/**
 * @author Pete Cornish
 */
data class HttpRoute(
    val path: String? = null,
    val regex: String? = null,
    val method: ResourceMethod? = null
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
