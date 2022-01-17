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
package io.gatehill.imposter.config.util

import org.apache.logging.log4j.LogManager
import java.nio.file.Path
import java.util.*
import kotlin.io.path.inputStream

/**
 * Wrapper for retrieving environment variables, allowing for
 * overrides.
 *
 * @author Pete Cornish
 */
class EnvVars(private val env: Map<String, String>) {
    companion object {
        /**
         * Precedes parsing, so uses system environment.
         */
        val discoverEnvFiles: Boolean = System.getenv("IMPOSTER_CONFIG_DISCOVER_ENVFILES")?.toBoolean() != false

        private val logger = LogManager.getLogger(EnvVars::class.java)
        private lateinit var INSTANCE: EnvVars

        init {
            reset(emptyList())
        }

        /**
         * Reset using the environment and any envfile.
         */
        fun reset(dotEnvPath: List<Path>) {
            val env = mutableMapOf<String, String>()
            dotEnvPath.forEach { env += loadEnvFile(it) }
            env += System.getenv()
            populate(env)
        }

        /**
         * Reads a file containing key-value pairs and removes any surrounding quotes from values,
         * representing both keys and values as [String]s.
         */
        private fun loadEnvFile(path: Path): Map<String, String> {
            logger.trace("Loading envfile: $path")
            return path.inputStream().use { stream ->
                return@use Properties().apply { this.load(stream) }
            }.entries.associate { (key, value) ->
                key.toString() to value.toString().removeSurrounding("\"")
            }
        }

        @JvmStatic
        fun populate(entries: Map<String, String>) {
            INSTANCE = EnvVars(entries)
        }

        @JvmStatic
        fun getEnv(): Map<String, String> {
            return INSTANCE.env
        }

        @JvmStatic
        fun getEnv(key: String): String? {
            return INSTANCE.env[key]
        }
    }
}
