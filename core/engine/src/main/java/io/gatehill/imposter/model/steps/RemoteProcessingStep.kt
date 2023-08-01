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

package io.gatehill.imposter.model.steps

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.http.HttpMethod
import io.gatehill.imposter.http.ResponseBehaviourFactory
import io.gatehill.imposter.plugin.config.capture.ItemCaptureConfig
import io.gatehill.imposter.plugin.config.resource.BasicResourceConfig
import io.gatehill.imposter.plugin.config.resource.ResponseConfig
import io.gatehill.imposter.script.ReadWriteResponseBehaviour
import io.gatehill.imposter.service.CaptureService
import io.gatehill.imposter.service.RemoteService
import io.gatehill.imposter.util.HttpUtil
import org.apache.logging.log4j.LogManager

class RemoteProcessingStep(
    private val remoteService: RemoteService,
    private val captureService: CaptureService,
) : ProcessingStep {
    private val logger = LogManager.getLogger(javaClass)

    override fun execute(
        responseBehaviourFactory: ResponseBehaviourFactory,
        resourceConfig: BasicResourceConfig,
        httpExchange: HttpExchange,
        statusCode: Int,
        context: StepContext,
    ): ReadWriteResponseBehaviour {
        val ctx = context as RemoteStepContext
        return try {
            val remoteExchange = remoteService.sendRequest(
                ctx.url,
                ctx.method,
                ctx.queryParams,
                ctx.headers,
                ctx.content,
                httpExchange
            )
            ctx.capture?.forEach { (key, config) ->
                captureService.captureItem(key, config, remoteExchange)
            }
            responseBehaviourFactory.build(statusCode, resourceConfig.responseConfig)
        } catch (e: Exception) {
            logger.error("Error sending remote request: {} {}", ctx.method, ctx.url, e)
            responseBehaviourFactory.build(HttpUtil.HTTP_INTERNAL_ERROR, ResponseConfig())
        }
    }
}

data class RemoteStepContext(
    val url: String,
    val method: HttpMethod,
    val queryParams: Map<String, String>?,
    val headers: Map<String, String>?,
    val content: String?,
    val capture: Map<String, ItemCaptureConfig>?,
) : StepContext
