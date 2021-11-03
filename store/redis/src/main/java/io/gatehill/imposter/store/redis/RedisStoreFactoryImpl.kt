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
package io.gatehill.imposter.store.redis

import com.google.inject.Inject
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.store.factory.AbstractStoreFactory
import io.gatehill.imposter.store.model.Store
import io.gatehill.imposter.store.redis.RedisStoreFactoryImpl
import org.apache.logging.log4j.LogManager
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import java.io.File
import java.io.IOException

/**
 * @author Pete Cornish
 */
class RedisStoreFactoryImpl @Inject constructor(imposterConfig: ImposterConfig) : AbstractStoreFactory() {
    private val redisson: RedissonClient

    init {
        val config: Config = try {
            val configFile = discoverConfigFile(imposterConfig)
            LOGGER.debug("Loading Redisson configuration from: {}", configFile)
            Config.fromYAML(configFile)
        } catch (e: IOException) {
            throw IllegalStateException("Unable to load Redisson configuration file", e)
        }
        redisson = Redisson.create(config)
    }

    private fun discoverConfigFile(imposterConfig: ImposterConfig): File {
        return imposterConfig.configDirs.map { dir: String? ->
            val configFile = File(dir, "redisson.yaml")
            if (configFile.exists()) {
                return@map configFile
            } else {
                return@map File(dir, "redisson.yml")
            }
        }.firstOrNull(File::exists) ?: throw IllegalStateException(
            "No Redisson configuration file named 'redisson.yaml' found in configuration directories"
        )
    }

    override fun buildNewStore(storeName: String): Store {
        return RedisStore(storeName, redisson)
    }

    companion object {
        private val LOGGER = LogManager.getLogger(
            RedisStoreFactoryImpl::class.java
        )
    }
}