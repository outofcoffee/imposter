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
package io.gatehill.imposter.config

import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.DefaultAwsRegionProviderChain
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.gatehill.imposter.config.util.EnvVars.Companion.getEnv
import org.apache.logging.log4j.LogManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.stream.Collectors

/**
 * Downloads a file from an S3 bucket.
 *
 * @author Pete Cornish
 */
class S3FileDownloader private constructor() {
    private val s3client: AmazonS3

    init {
        val clientBuilder = AmazonS3ClientBuilder.standard().enablePathStyleAccess()
        System.getProperty(SYS_PROP_S3_API_ENDPOINT, getEnv(ENV_S3_API_ENDPOINT))
            ?.let { s3Endpoint: String ->
                clientBuilder.withEndpointConfiguration(
                    EndpointConfiguration(s3Endpoint, DefaultAwsRegionProviderChain().region)
                )
            }
        s3client = clientBuilder.build()
    }

    /**
     * @param s3Url an S3 URL in the form `s3://bucket_name/key_name`
     * @return the contents of the file
     */
    fun readFileFromS3(s3Url: String): String {
        try {
            val bucketName = determineBucketName(s3Url)
            val keyName = determineObjectKeyPrefix(s3Url)

            val content: String = s3client.getObject(bucketName, keyName).objectContent.use { s3is ->
                BufferedReader(InputStreamReader(s3is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"))
            }

            LOGGER.debug("File read [${content.length} bytes] from S3: $s3Url")
            return content

        } catch (e: Exception) {
            throw RuntimeException("Error fetching file from S3: $s3Url", e)
        }
    }

    /**
     * Lists files, recursively, at the given S3 location.
     * @param s3Url an S3 URL in the form `s3://bucket_name/key_name`
     * @return the file names, relative to `s3Url`
     */
    fun listFiles(s3Url: String): List<String> {
        try {
            val bucketName = determineBucketName(s3Url)
            val keyName = determineObjectKeyPrefix(s3Url)

            val objects = s3client.listObjectsV2(bucketName, keyName).objectSummaries
                .map { it.key.substring(keyName.length) }

            LOGGER.debug("Found ${objects.size} objects in S3: $s3Url: $objects")
            return objects

        } catch (e: Exception) {
            throw RuntimeException("Error fetching file from S3: $s3Url", e)
        }
    }

    fun downloadAllFiles(s3BaseUrl: String, destDir: File) {
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw RuntimeException("Unable to create destination directory: $destDir")
        }

        val normalisedBaseUrl = s3BaseUrl.removeSuffix("/")

        val files: List<String> = listFiles(normalisedBaseUrl)
        if (files.isEmpty()) {
            throw IllegalStateException("No files found in S3 at: $normalisedBaseUrl")
        }

        files.map { it.removePrefix("/") }.filter { it.isNotEmpty() }.forEach { fileName ->
            val s3Url = normalisedBaseUrl.removeSuffix("/") + "/" + fileName
            val localFile = File(destDir, fileName)

            // check if in subdir
            if (fileName.contains('/')) {
                val subDirs = fileName.substring(0, fileName.lastIndexOf('/'))
                try {
                    val localSubDirs = File(destDir, subDirs)
                    if (localSubDirs.exists()) {
                        if (!localSubDirs.isDirectory) {
                            throw IllegalStateException("Unable to create $localSubDirs - path exists but is not a directory")
                        }
                    } else {
                        localSubDirs.mkdirs()
                    }
                } catch (e: Exception) {
                    throw RuntimeException("Error creating subdirectories: $subDirs", e)
                }
            }

            // only fetch files
            if (!fileName.endsWith("/")) {
                try {
                    val content = readFileFromS3(s3Url)
                    localFile.writeText(content)
                    LOGGER.debug("Downloaded file: $s3Url [${content.length} bytes] to: $localFile")
                } catch (e: Exception) {
                    throw RuntimeException("Error downloading file: $s3Url to: $localFile", e)
                }
            }
        }
    }

    /**
     * @param s3Url an S3 URL in the form `s3://bucket_name/key_name` or `s3://bucket_name`
     * @return the bucket name
     */
    internal fun determineBucketName(s3Url: String): String {
        val normalisedUrl = s3Url.removeSuffix("/")
        val slashIndex = normalisedUrl.indexOf("/", S3PROTO_PREFIX.length)
        return normalisedUrl.substring(
            S3PROTO_PREFIX.length,
            if (slashIndex == -1) normalisedUrl.length else slashIndex
        )
    }

    private fun determineObjectKeyPrefix(s3Url: String): String {
        val normalisedUrl = s3Url.removeSuffix("/")
        val bucketName = determineBucketName(s3Url)
        return if (normalisedUrl.length == bucketName.length + S3PROTO_PREFIX.length) {
            ""
        } else {
            normalisedUrl.substring(bucketName.length + S3PROTO_PREFIX.length + 1)
        }
    }

    companion object {
        private const val S3PROTO_PREFIX = "s3://"
        const val ENV_S3_API_ENDPOINT = "IMPOSTER_S3_API_ENDPOINT"
        const val SYS_PROP_S3_API_ENDPOINT = "imposter.s3.api.endpoint"
        private val LOGGER = LogManager.getLogger(S3FileDownloader::class.java)

        private var instance: S3FileDownloader? = null

        fun getInstance(): S3FileDownloader {
            return instance ?: run {
                instance = S3FileDownloader()
                instance!!
            }
        }

        /**
         * Clears the instance holding the S3 client. Typically only called from tests.
         */
        @JvmStatic
        fun destroyInstance() {
            instance = null
        }
    }
}
