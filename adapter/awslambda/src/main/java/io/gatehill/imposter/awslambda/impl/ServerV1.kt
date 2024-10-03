/*
 * Copyright (c) 2022-2023.
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

package io.gatehill.imposter.awslambda.impl

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.gatehill.imposter.awslambda.impl.model.LambdaHttpRequestV1
import io.gatehill.imposter.awslambda.impl.model.LambdaHttpResponse
import io.gatehill.imposter.http.HttpRoute
import io.gatehill.imposter.http.HttpRouter
import io.gatehill.imposter.service.ResponseService
import io.gatehill.imposter.util.HttpUtil

/**
 * Server for API Gateway v1 events.
 *
 * @author Pete Cornish
 */
class ServerV1(
    responseService: ResponseService,
    private val router: HttpRouter,
) : LambdaServer<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>(responseService, router) {

    init {
        logger.debug("Configured for API Gateway v1 events")
    }

    override fun getRequestMethod(event: APIGatewayProxyRequestEvent): String = event.httpMethod
    override fun getRequestPath(event: APIGatewayProxyRequestEvent): String = event.path

    override fun acceptsHtml(event: APIGatewayProxyRequestEvent): Boolean {
        // TODO handle wildcard mime types, not just exact matches
        return HttpUtil.readAcceptedContentTypes(event.headers["Accept"]).contains(HttpUtil.CONTENT_TYPE_HTML)
    }

    override fun buildRequest(event: APIGatewayProxyRequestEvent, route: HttpRoute?) = LambdaHttpRequestV1(event, router, route)

    override fun buildResponse(response: LambdaHttpResponse) = APIGatewayProxyResponseEvent().apply {
        // read status again in case modified by error handler
        statusCode = response.statusCode

        headers = response.headers

        if (response.bodyLength > 0) {
            // TODO encode to base 64 if request.event.isBase64Encoded == true
            body = response.bodyBuffer?.toString(Charsets.UTF_8)
            isBase64Encoded = false
        }
    }
}
