/*
 * Copyright (c) 2016-2023.
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
package io.gatehill.imposter.server.vertxweb.impl

import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.server.vertxweb.util.VertxResourceUtil
import io.gatehill.imposter.util.CollectionUtil
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

/**
 * @author Pete Cornish
 */
class VertxHttpRequest(
    private val routingContext: RoutingContext,
) : HttpRequest {
    private val vertxRequest = routingContext.request()
    private val _queryParams by lazy { CollectionUtil.asMap(routingContext.queryParams()) }
    private val _formParams by lazy { CollectionUtil.asMap(vertxRequest.formAttributes()) }
    private val _headers by lazy { CollectionUtil.asMap(vertxRequest.headers()) }

    override val path: String
        get() = vertxRequest.path() ?: ""

    override val method: HttpMethod
        get() = VertxResourceUtil.convertMethodFromVertx(vertxRequest.method())

    override val absoluteUri: String
        get() = vertxRequest.absoluteURI()

    override val headers: Map<String, String>
        get() = _headers

    override fun getHeader(headerKey: String): String? {
        return vertxRequest.getHeader(headerKey)
    }

    override val pathParams: Map<String, String>
        get() = routingContext.pathParams()

    override fun getPathParam(paramName: String): String? {
        return routingContext.pathParam(paramName)
    }

    override val queryParams: Map<String, String>
        get() = _queryParams

    override fun getQueryParam(queryParam: String): String? {
        return routingContext.queryParam(queryParam)?.firstOrNull()
    }

    override val formParams: Map<String, String>
        get() = _formParams

    override fun getFormParam(formParam: String): String? {
        return vertxRequest.getFormAttribute(formParam)
    }

    /**
     * Vert.x Web caches the body buffer as of 4.3
     */
    override val body: Buffer?
        get() = routingContext.body().buffer()

    /**
     * Vert.x Web caches the body string as of 4.3
     */
    override val bodyAsString: String?
        get() = routingContext.body().asString()

    /**
     * Vert.x Web caches the body JsonObject as of 4.3
     */
    override val bodyAsJson: JsonObject?
        get() = routingContext.body().asJsonObject()
}
