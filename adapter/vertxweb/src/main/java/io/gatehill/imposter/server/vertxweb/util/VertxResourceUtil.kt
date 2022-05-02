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

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import io.gatehill.imposter.http.HttpMethod

/**
 * @author Pete Cornish
 */
object VertxResourceUtil {
    private val METHODS: BiMap<HttpMethod, io.vertx.core.http.HttpMethod?> = HashBiMap.create()

    /**
     * Converts [io.gatehill.imposter.http.HttpMethod]s to [io.vertx.core.http.HttpMethod]s.
     */
    fun convertMethodToVertx(method: HttpMethod): io.vertx.core.http.HttpMethod =
        METHODS[method] ?: throw UnsupportedOperationException("Unknown method: $method")

    fun convertMethodFromVertx(method: io.vertx.core.http.HttpMethod): HttpMethod =
        METHODS.inverse()[method] ?: throw UnsupportedOperationException("Unknown method: $method")

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
}
