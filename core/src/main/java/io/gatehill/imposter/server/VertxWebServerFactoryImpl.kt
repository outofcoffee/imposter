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
package io.gatehill.imposter.server

import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.server.VertxWebServerFactoryImpl
import io.gatehill.imposter.util.AsyncUtil
import io.gatehill.imposter.util.FileUtil
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.JksOptions
import io.vertx.ext.web.Router
import org.apache.logging.log4j.LogManager
import java.net.URISyntaxException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Pete Cornish
 */
class VertxWebServerFactoryImpl : ServerFactory {
    override fun provide(
        imposterConfig: ImposterConfig,
        startFuture: Future<*>,
        vertx: Vertx,
        router: Router
    ): HttpServer {
        LOGGER.trace("Starting mock server on {}:{}", imposterConfig.host, imposterConfig.listenPort)
        val serverOptions = HttpServerOptions()

        // configure keystore and enable HTTPS
        if (imposterConfig.isTlsEnabled) {
            LOGGER.trace("TLS is enabled")

            // locate keystore
            val keystorePath: Path
            keystorePath = if (imposterConfig.keystorePath!!.startsWith(FileUtil.CLASSPATH_PREFIX)) {
                try {
                    Paths.get(
                        VertxWebServerFactoryImpl::class.java.getResource(
                            imposterConfig.keystorePath!!.substring(FileUtil.CLASSPATH_PREFIX.length)
                        ).toURI()
                    )
                } catch (e: URISyntaxException) {
                    throw RuntimeException("Error locating keystore", e)
                }
            } else {
                Paths.get(imposterConfig.keystorePath)
            }
            val jksOptions = JksOptions()
            jksOptions.path = keystorePath.toString()
            jksOptions.password = imposterConfig.keystorePassword
            serverOptions.keyStoreOptions = jksOptions
            serverOptions.isSsl = true
        } else {
            LOGGER.trace("TLS is disabled")
        }
        LOGGER.trace("Listening on {}", imposterConfig.serverUrl)
        return vertx.createHttpServer(serverOptions)
            .requestHandler(router)
            .listen(imposterConfig.listenPort, imposterConfig.host, AsyncUtil.resolveFutureOnCompletion(startFuture))
    }

    companion object {
        private val LOGGER = LogManager.getLogger(
            VertxWebServerFactoryImpl::class.java
        )
    }
}