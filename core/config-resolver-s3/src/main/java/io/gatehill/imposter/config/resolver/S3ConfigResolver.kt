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
package io.gatehill.imposter.config.resolver

import io.gatehill.imposter.config.S3FileDownloader
import org.apache.logging.log4j.LogManager
import java.io.File
import java.nio.file.Files

/**
 * Downloads all configuration files from a given S3 path to a local directory,
 * then returns the path to the local directory.
 */
class S3ConfigResolver : ConfigResolver {
    private val logger = LogManager.getLogger(S3ConfigResolver::class.java)

    /**
     * Always retrieve the instance, in case it is stale.
     */
    private val s3FileDownloader
        get() = S3FileDownloader.getInstance()

    override fun handles(configPath: String): Boolean {
        return configPath.startsWith("s3://")
    }

    override fun resolve(configPath: String): File {
        val bucketName = s3FileDownloader.determineBucketName(configPath)
        val localConfigDir = Files.createTempDirectory("s3-$bucketName").toFile()

        logger.debug("Retrieving configuration from $configPath")
        s3FileDownloader.downloadAllFiles(configPath, localConfigDir)
        return localConfigDir
    }
}
