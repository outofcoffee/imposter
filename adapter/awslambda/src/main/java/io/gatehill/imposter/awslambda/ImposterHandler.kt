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
import io.gatehill.imposter.awslambda.config.Settings
import io.gatehill.imposter.awslambda.impl.LambdaServer
import io.gatehill.imposter.awslambda.impl.LambdaServerFactory
import io.gatehill.imposter.awslambda.util.ImposterBuilderKt
import io.gatehill.imposter.awslambda.util.LambdaPlugin
import io.gatehill.imposter.embedded.MockEngine
import io.gatehill.imposter.plugin.openapi.OpenApiPluginImpl
import io.gatehill.imposter.plugin.openapi.loader.S3FileDownloader
import io.gatehill.imposter.plugin.rest.RestPluginImpl
import io.gatehill.imposter.server.RequestHandlingMode
import io.gatehill.imposter.util.InjectorUtil
import org.apache.logging.log4j.LogManager

/**
 * AWS Lambda handler.
 *
 * @author Pete Cornish
 */
class ImposterHandler : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private val logger = LogManager.getLogger(ImposterHandler::class.java)
    private val engine: MockEngine
    private val server: LambdaServer

    init {
        // lambda functions are only allowed write access to /tmp
        System.setProperty("vertx.cacheDirBase", "/tmp/.vertx")
        System.setProperty("java.io.tmpdir", "/tmp")

        logger.debug("Retrieving configuration from ${Settings.s3ConfigUrl}")
        S3FileDownloader.getInstance().downloadAllFiles(Settings.s3ConfigUrl, Settings.configDir)

        engine = ImposterBuilderKt()
            .withPluginClass(LambdaPlugin::class.java)
            .withPluginClass(OpenApiPluginImpl::class.java)
            .withPluginClass(RestPluginImpl::class.java)
            .withConfigurationDir(Settings.configDir.path)
            .withEngineOptions { options ->
                options.serverFactory = LambdaServerFactory::class.qualifiedName
                options.requestHandlingMode = RequestHandlingMode.SYNC
            }.startBlocking()

        val serverFactory = InjectorUtil.injector!!.getInstance(LambdaServerFactory::class.java)
        server = serverFactory.activeServer
    }

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        try {
            return server.dispatch(input)
        } catch (e: Exception) {
            logger.error(e)
            return APIGatewayProxyResponseEvent().withStatusCode(500)
        }
    }
}
