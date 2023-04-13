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

import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpRequest
import io.gatehill.imposter.http.HttpResponse
import io.gatehill.imposter.http.HttpRoute
import io.gatehill.imposter.http.HttpRouter
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.impl.ParsableMIMEValue

/**
 * @author Pete Cornish
 */
class VertxHttpExchange(
    private val router: HttpRouter,
    internal val routingContext: RoutingContext,
    override val currentRoute: HttpRoute?,
) : HttpExchange {
    override var phase = ExchangePhase.REQUEST_RECEIVED

    override val request: HttpRequest by lazy {
        VertxHttpRequest(routingContext)
    }

    override val response: HttpResponse by lazy {
        VertxHttpResponse(router, this, routingContext.response())
    }

    override fun isAcceptHeaderEmpty(): Boolean {
        return routingContext.parsedHeaders().accept().isEmpty()
    }

    override fun acceptsMimeType(mimeType: String): Boolean {
        val mimeValue = ParsableMIMEValue(mimeType)
        return routingContext.parsedHeaders().accept().any { it.isMatchedBy(mimeValue) }
    }

    override fun fail(cause: Throwable?) {
        routingContext.fail(cause)
    }

    override fun fail(statusCode: Int) {
        routingContext.fail(statusCode)
    }

    override fun fail(statusCode: Int, cause: Throwable?) {
        routingContext.fail(statusCode, cause)
    }

    override fun failure(): Throwable? {
        return routingContext.failure()
    }

    override fun <T> get(key: String): T? {
        return routingContext.get<T>(key)
    }

    override fun put(key: String, value: Any) {
        routingContext.put(key, value)
    }
}
