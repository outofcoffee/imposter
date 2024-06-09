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
package io.gatehill.imposter.server.vertxweb.util

import com.google.common.base.Strings
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRoute
import java.util.*

/**
 * @author Pete Cornish
 */
object VertxResourceUtil {
    /**
     * Vert.x documentation says:
     * > The placeholders consist of : followed by the parameter name.
     * > Parameter names consist of any alphabetic character, numeric character or underscore.
     *
     * See: https://vertx.io/docs/vertx-web/java/#_capturing_path_parameters
     *
     * This regex pattern does not include the colon prefix.
     */
    private val VERTX_PATH_PARAM_NAME = Regex("[a-zA-Z0-9_]+")

    private val METHODS: BiMap<HttpMethod, io.vertx.core.http.HttpMethod?> = HashBiMap.create()

    init {
        METHODS[HttpMethod.GET] = io.vertx.core.http.HttpMethod.GET
        METHODS[HttpMethod.HEAD] = io.vertx.core.http.HttpMethod.HEAD
        METHODS[HttpMethod.POST] = io.vertx.core.http.HttpMethod.POST
        METHODS[HttpMethod.PUT] = io.vertx.core.http.HttpMethod.PUT
        METHODS[HttpMethod.PATCH] = io.vertx.core.http.HttpMethod.PATCH
        METHODS[HttpMethod.DELETE] = io.vertx.core.http.HttpMethod.DELETE
        METHODS[HttpMethod.CONNECT] = io.vertx.core.http.HttpMethod.CONNECT
        METHODS[HttpMethod.OPTIONS] = io.vertx.core.http.HttpMethod.OPTIONS
        METHODS[HttpMethod.TRACE] = io.vertx.core.http.HttpMethod.TRACE
    }

    /**
     * Converts [io.gatehill.imposter.http.HttpMethod]s to [io.vertx.core.http.HttpMethod]s.
     */
    fun HttpMethod.convertMethodToVertx(): io.vertx.core.http.HttpMethod =
        METHODS[this] ?: throw UnsupportedOperationException("Unknown method: $this")

    fun io.vertx.core.http.HttpMethod.convertMethodFromVertx(): HttpMethod =
        METHODS.inverse()[this] ?: throw UnsupportedOperationException("Unknown method: $this")

    /**
     * Normalises path parameters to Vert.x format.
     *
     * If a path parameter name does not match the Vert.x format, it is replaced with a UUID,
     * and a mapping is added to the `normalisedParams` map.
     *
     * For example:
     * ```
     * /{pathParam}/notParam
     * ```
     * will be converted to:
     * ```
     * /:pathParam/notParam
     * ```
     *
     * A path parameter name that does not match the Vert.x format, such as:
     * ```
     * /{param-with-dashes}
     * ```
     * will be converted to:
     * ```
     * /:123e4567e89b12d3a4564266141740000
     * ```
     * and the mapping `param-with-dashes -> 123e4567e89b12d3a4564266141740000`
     * will be added to the `normalisedParams` map.
     *
     * @param normalisedParams a map to store the normalised parameter names
     * @param rawPath the path to normalise
     */
    fun normalisePath(normalisedParams: MutableMap<String, String>, rawPath: String): String {
        var path = rawPath
        if (!Strings.isNullOrEmpty(path)) {
            var matchFound: Boolean
            do {
                val matcher = HttpRoute.PATH_PARAM_PLACEHOLDER.matcher(path)
                matchFound = matcher.find()
                if (matchFound) {
                    val finalParamName = matcher.group(1).let { paramName ->
                        if (paramName.matches(VERTX_PATH_PARAM_NAME)) {
                            paramName
                        } else {
                            val existingMapping = normalisedParams.entries.find { it.value == paramName }
                            if (null != existingMapping) {
                                return@let existingMapping.key
                            }
                            val normalisedName = UUID.randomUUID().toString().replace("-", "")
                            normalisedParams[normalisedName] = paramName
                            normalisedName
                        }
                    }
                    path = matcher.replaceFirst(":$finalParamName")
                }
            } while (matchFound)
        }
        return path
    }

    fun getNormalisedParamName(normalisedParams: Map<String, String>, originalParamName: String): String {
        if (originalParamName.matches(VERTX_PATH_PARAM_NAME)) {
            return originalParamName
        }
        return normalisedParams.entries.find { it.value == originalParamName }?.key ?: originalParamName
    }

    fun denormaliseParams(normalisedParams: Map<String, String>, vertxParams: Map<String, String>): Map<String, String> {
        // if it's not in the map it doesn't need to be denormalised
        return vertxParams.mapKeys { normalisedParams[it.key] ?: it.key }
    }
}
