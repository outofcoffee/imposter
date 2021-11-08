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
package io.gatehill.imposter.util

import com.google.common.base.Strings
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import io.gatehill.imposter.plugin.config.ContentTypedConfig
import io.gatehill.imposter.plugin.config.resource.MethodResourceConfig
import io.gatehill.imposter.plugin.config.resource.ResourceMethod
import io.vertx.core.http.HttpMethod
import java.util.*
import java.util.regex.Pattern

/**
 * @author Pete Cornish
 */
object ResourceUtil {
    const val RESPONSE_CONFIG_HOLDER_KEY = "io.gatehill.imposter.responseConfigHolder"
    const val RC_REQUEST_ID_KEY = "request.id"
    private val METHODS: BiMap<ResourceMethod, HttpMethod?> = HashBiMap.create()
    private val PATH_PARAM_PLACEHOLDER = Pattern.compile("\\{([a-zA-Z0-9._\\-]+)}")

    /**
     * Converts [ResourceMethod]s to [HttpMethod]s.
     */
    @JvmStatic
    fun convertMethodToVertx(resourceConfig: ContentTypedConfig?): HttpMethod {
        return if (resourceConfig is MethodResourceConfig) {
            val method = (resourceConfig as MethodResourceConfig).method ?: ResourceMethod.GET
            METHODS[method] ?: throw UnsupportedOperationException("Unknown method: $method")
        } else {
            HttpMethod.GET
        }
    }

    fun convertMethodFromVertx(method: HttpMethod?): ResourceMethod {
        return METHODS.inverse()[method] ?: throw UnsupportedOperationException("Unknown method: $method")
    }

    /**
     * Convert the OpenAPI style path to a Vert.x path, including any parameter placeholders.
     *
     * For example:
     * `
     * /example/{foo}
    ` *
     *
     * will be converted to:
     * `
     * /example/:foo
    ` *
     *
     * @param path the OpenAPI path
     * @return the Vert.x path
     */
    @JvmStatic
    fun convertPathToVertx(openapiPath: String?): String? {
        var path = openapiPath
        if (!Strings.isNullOrEmpty(path)) {
            var matchFound: Boolean
            do {
                val matcher = PATH_PARAM_PLACEHOLDER.matcher(path)
                matchFound = matcher.find()
                if (matchFound) {
                    path = matcher.replaceFirst(":" + matcher.group(1))
                }
            } while (matchFound)
        }
        return path
    }

    init {
        METHODS[ResourceMethod.GET] = HttpMethod.GET
        METHODS[ResourceMethod.HEAD] =
            HttpMethod.HEAD
        METHODS[ResourceMethod.POST] =
            HttpMethod.POST
        METHODS[ResourceMethod.PUT] =
            HttpMethod.PUT
        METHODS[ResourceMethod.PATCH] = HttpMethod.PATCH
        METHODS[ResourceMethod.DELETE] =
            HttpMethod.DELETE
        METHODS[ResourceMethod.CONNECT] = HttpMethod.CONNECT
        METHODS[ResourceMethod.OPTIONS] = HttpMethod.OPTIONS
        METHODS[ResourceMethod.TRACE] = HttpMethod.TRACE
    }
}