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
package io.gatehill.imposter.util

import com.google.common.base.Strings
import io.gatehill.imposter.config.util.EnvVars
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

/**
 * @author Pete Cornish
 */
object FileUtil {
    const val CLASSPATH_PREFIX = "classpath:"

    val engineCacheDir: Path by lazy {
        val cacheDirPath = (EnvVars.getEnv("IMPOSTER_CACHE_DIR")?.let { Paths.get(it) }
            ?: Paths.get(System.getProperty("java.io.tmpdir"), "imposter-cache"))

        if (!cacheDirPath.exists()) {
            cacheDirPath.createDirectories()
        }
        return@lazy cacheDirPath
    }

    /**
     * Return the row with the given ID.
     *
     * @param idFieldName
     * @param rowId
     * @param rows
     * @return
     */
    fun findRow(idFieldName: String?, rowId: String?, rows: JsonArray): JsonObject? {
        check(!Strings.isNullOrEmpty(idFieldName)) { "ID field name not configured" }
        for (i in 0 until rows.size()) {
            val row = rows.getJsonObject(i)
            if (row.getValue(idFieldName)?.toString()?.equals(rowId, ignoreCase = true) == true) {
                return row
            }
        }
        return null
    }

    /**
     * Validates a file path to ensure it is within the config directory.
     *
     * @param path the path to validate
     * @param configDir the configuration directory
     */
    fun validatePath(path: String, configDir: File): Path {
        val basePath = configDir.canonicalFile.toPath()
        val resolvedPath = Paths.get(configDir.absolutePath, path).normalize()

        // Ensure the resolved path is within the base directory
        if (!resolvedPath.startsWith(basePath)) {
            throw SecurityException("Access denied: Path '$path' attempts to escape the config directory")
        }
        return resolvedPath
    }
}
