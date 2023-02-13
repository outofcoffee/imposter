/*
 * Copyright (c) 2021-2023.
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

package io.gatehill.imposter.awslambda.impl.model

import com.google.common.base.Strings
import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.http.HttpRoute
import io.gatehill.imposter.util.HttpUtil

/**
 * @author Pete Cornish
 */
class LambdaHttpExchange(
    override val request: HttpRequest,
    override val response: HttpResponse,
    private val currentRoute: HttpRoute?,
) : HttpExchange {
    override var phase = ExchangePhase.REQUEST_RECEIVED
    private val attributes = mutableMapOf<String, Any>()
    private var failureCause: Throwable? = null

    private val acceptedMimeTypes: List<String> by lazy {
        HttpUtil.readAcceptedContentTypes(this@LambdaHttpExchange)
    }

    override val currentRoutePath: String?
        get() = currentRoute?.path

    override fun isAcceptHeaderEmpty(): Boolean {
        return Strings.isNullOrEmpty(request.getHeader("Accept"))
    }

    override fun acceptsMimeType(mimeType: String): Boolean {
        // TODO handle wildcard mime types, not just exact matches
        return acceptedMimeTypes.contains(mimeType)
    }

    override fun fail(cause: Throwable?) {
        fail(500, cause)
    }

    override fun fail(statusCode: Int) {
        response.setStatusCode(statusCode)
    }

    override fun fail(statusCode: Int, cause: Throwable?) {
        response.setStatusCode(statusCode)
        failureCause = cause
    }

    override fun failure(): Throwable? {
        return failureCause
    }

    override fun <T> get(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return attributes[key] as T?
    }

    override fun put(key: String, value: Any) {
        attributes[key] = value
    }
}
