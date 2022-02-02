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
package io.gatehill.imposter.service

import io.vertx.core.Vertx
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import javax.inject.Inject

/**
 * [Channel]-based executor for deferring operations to a background worker thread.
 *
 * @author Pete Cornish
 */
class DeferredOperationService @Inject constructor(
    private val vertx: Vertx,
) {
    private val logger: Logger = LogManager.getLogger(DeferredOperationService::class.java)
    private val deferredOperations: Channel<Pair<String, Runnable>> by lazy { startDeferredExecutor() }

    fun defer(description: String, deferred: Runnable) = runBlocking {
        logger.trace("Enqueuing deferred operation: $description")
        deferredOperations.send(description to deferred)
    }

    private fun startDeferredExecutor(): Channel<Pair<String, Runnable>> {
        logger.debug("Starting deferred executor")
        val channel = Channel<Pair<String, Runnable>>(512)

        vertx.executeBlocking<Unit>({
            runBlocking {
                while (true) {
                    val (description, deferred) = channel.receive()
                    logger.trace("Dequeued deferred operation: $description")
                    try {
                        deferred.run()
                    } catch (e: Exception) {
                        logger.error("Deferred operation '$description' failed", e)
                    }
                }
            }
        }, {
            if (it.failed()) {
                logger.error("Terminated deferred executor", it.cause())
            } else {
                logger.debug("Terminated deferred executor normally")
            }
        })

        return channel
    }
}
