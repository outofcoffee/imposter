/*
 * Copyright (c) 2021-2021.
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

package io.gatehill.imposter.awslambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import io.gatehill.imposter.awslambda.impl.LambdaServerFactory

/**
 * AWS Lambda handler accepting event type for API Gateway V1 REST APIs.
 *
 * @author Pete Cornish
 */
class Handler : AbstractHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>(
    LambdaServerFactory.EventType.ApiGatewayV1
), RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        val response = try {
            if (logger.isTraceEnabled) {
                logger.trace("Received request: $input")
            } else {
                logger.info("Received request: ${input.httpMethod} ${input.path}")
            }
            server.dispatch(input)

        } catch (e: Exception) {
            logger.error(e)
            APIGatewayProxyResponseEvent().withStatusCode(500)
        }

        if (logger.isTraceEnabled) {
            logger.trace("Sending response: $response")
        } else {
            logger.info("Sending response: [statusCode=${response.statusCode},body=<${response.body?.let { "${it.length} bytes" } ?: "null"}>]")
        }
        return response
    }
}
