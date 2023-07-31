/*
 * Copyright (c) 2023-2023.
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

package io.gatehill.imposter.model.steps.http

import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRoute
import okhttp3.Request
import okhttp3.Response

/**
 * Adapts an OkHttp HTTP request and response to an Imposter HTTP exchange.
 */
class RemoteHttpExchange(
    private val initiatingExchange: HttpExchange,
    private val remoteReq: Request,
    private val remoteResp: Response,
    private val remoteRespBody: String?,
) : HttpExchange {
    override var phase = ExchangePhase.RESPONSE_SENT

    override val request
        get() = RemoteHttpRequest(remoteReq)

    override val response
        get() = RemoteHttpResponse(remoteResp, remoteRespBody)

    override fun isAcceptHeaderEmpty(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun acceptsMimeType(mimeType: String): Boolean {
        throw UnsupportedOperationException()
    }

    override val currentRoute: HttpRoute?
        get() = throw UnsupportedOperationException()

    override fun fail(cause: Throwable?) {
        throw UnsupportedOperationException()
    }

    override fun fail(statusCode: Int) {
        throw UnsupportedOperationException()
    }

    override fun fail(statusCode: Int, cause: Throwable?) {
        throw UnsupportedOperationException()
    }

    override fun failure(): Throwable? {
        throw UnsupportedOperationException()
    }

    override fun <T> get(key: String): T? {
        return initiatingExchange.get<T>(key)
    }

    override fun put(key: String, value: Any) {
        initiatingExchange.put(key, value)
    }
}
