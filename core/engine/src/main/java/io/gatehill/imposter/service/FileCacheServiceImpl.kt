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
package io.gatehill.imposter.service

import com.google.common.io.BaseEncoding
import io.gatehill.imposter.util.FileUtil
import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import java.nio.file.Paths
import java.security.MessageDigest
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeText

/**
 * Simple local filesystem cache.
 *
 * @author Pete Cornish
 */
class FileCacheServiceImpl : FileCacheService {
    private val logger = LogManager.getLogger(ResourceServiceImpl::class.java)

    private val mutex = Any()

    override fun readFromCache(cacheKey: String): FileCacheService.CacheResult {
        val cachedPath = generateCachedPath(cacheKey)
        if (!cachedPath.exists()) {
            return FileCacheService.CacheResult(false)
        }
        val content = synchronized(mutex) {
            // avoid race condition of multiple writes or partial write and concurrent read
            cachedPath.readBytes()
        }
        logger.trace("Read cached file: {} [{} bytes] with key: {}", cachedPath, content.size, cacheKey)
        return FileCacheService.CacheResult(true, content)
    }

    override fun writeToCache(cacheKey: String, content: String) {
        val cachedPath = generateCachedPath(cacheKey)
        logger.trace("Writing cached file [{} bytes]: {} with key: {}", content.length, cachedPath, cacheKey)
        synchronized(mutex) {
            // avoid race condition of multiple writes or partial write and concurrent read
            cachedPath.writeText(content)
        }
    }

    private fun generateCachedPath(cacheKey: String): Path =
        Paths.get(FileUtil.engineCacheDir.absolutePathString(), hashKey(cacheKey))

    /**
     * @return hashed, hex encoded representation of the cache key
     */
    private fun hashKey(cacheKey: String): String =
        BaseEncoding.base16().encode(MessageDigest.getInstance("SHA-256").digest(cacheKey.toByteArray()))
}
