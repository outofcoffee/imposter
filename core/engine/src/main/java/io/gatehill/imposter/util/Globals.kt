/*
 * Copyright (c) 2016-2024.
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
package io.gatehill.imposter.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.CompletableFuture

fun getJvmVersion(): Int {
    var version = System.getProperty("java.version")
    if (version.startsWith("1.")) {
        version = version.substring(2, 3)
    } else {
        val dot = version.indexOf(".")
        if (dot != -1) {
            version = version.substring(0, dot)
        }
    }
    return version.toInt()
}

/**
 * Wraps the given [block] in a `CompletableFuture` and returns the future.
 * If [autoComplete] is `true`, the future will be completed with the result of the block.
 * If [autoComplete] is `false`, the future must be completed manually by the block code.
 * This is useful for long-running tasks that need to be managed externally.
 */
fun <T>makeFuture(autoComplete: Boolean = true, block: (CompletableFuture<T>) -> T): CompletableFuture<T> {
    val future = CompletableFuture<T>()
    try {
        val ret = block(future)
        if (autoComplete) {
            future.complete(ret)
        }
    } catch (e: Exception) {
        future.completeExceptionally(e)
    }
    return future
}

fun completedUnitFuture(): CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)

val supervisedDefaultCoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
val supervisedIOCoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
