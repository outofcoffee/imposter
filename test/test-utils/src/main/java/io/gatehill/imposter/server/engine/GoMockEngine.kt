/*
 * Copyright (c) 2025-2025.
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

package io.gatehill.imposter.server.engine

import io.gatehill.imposter.config.ConfigHolder
import io.gatehill.imposter.util.HttpUtil
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import org.apache.logging.log4j.LogManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Go-based mock engine.
 */
class GoMockEngine : TestMockEngine {
    private val logger = LogManager.getLogger(GoMockEngine::class.java)
    private var engineProcess: Process? = null

    override fun start(vertx: Vertx, host: String, testContext: VertxTestContext) {
        // full path to the 'imposter-go' binary
        val binaryPath = System.getenv("IMPOSTER_GO_PATH")
        if (binaryPath.isNullOrBlank()) {
            testContext.failNow("IMPOSTER_GO_PATH environment variable not set")
            return
        }
        logger.trace("Using imposter-go binary: $binaryPath")

        // invoke the 'imposter-go' binary on the path and pass the config directory
        engineProcess = ProcessBuilder(binaryPath)
            .apply {
                val env = environment()
                env["IMPOSTER_PORT"] = ConfigHolder.config.listenPort.toString()
                env["IMPOSTER_CONFIG_DIR"] = ConfigHolder.config.configDirs.joinToString(",")
                env["IMPOSTER_SUPPORT_LEGACY_CONFIG"] = "true"
                env["IMPOSTER_LOG_LEVEL"] = "trace"
            }
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        waitForEngine(host, testContext)
    }

    private fun waitForEngine(host: String, testContext: VertxTestContext) {
        val timeout = 5000
        val checkInterval = 50L

        // wait for /system/status to return 200
        val statusUrl = URI.create("http://$host:${ConfigHolder.config.listenPort}/system/status")
        val startTime = System.currentTimeMillis()
        var lastException: Exception? = null
        while (System.currentTimeMillis() - startTime < timeout) {
            logger.debug("Checking engine status...")
            try {
                val response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder().uri(statusUrl).build(),
                    HttpResponse.BodyHandlers.discarding()
                )
                if (response.statusCode() == HttpUtil.HTTP_OK) {
                    logger.debug("Engine ready")
                    testContext.completeNow()
                    return
                }
            } catch (e: Exception) {
                lastException = e
                logger.trace("Engine not ready ($e)")
            }
            // check if process has terminated
            if (!engineProcess!!.isAlive) {
                lastException = RuntimeException("Engine process terminated")
                break
            }
            Thread.sleep(checkInterval)
        }
        val startEx = RuntimeException("Engine did not start after $timeout ms", lastException)
        logger.error(startEx)
        testContext.failNow(startEx)
    }

    override fun stop() {
        logger.trace("Stopping engine with pid ${engineProcess?.pid()}")
        engineProcess?.destroy()
    }
}
