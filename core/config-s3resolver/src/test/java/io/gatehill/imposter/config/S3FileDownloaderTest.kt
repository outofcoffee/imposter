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

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import com.amazonaws.SDKGlobalConfiguration
import com.amazonaws.regions.Regions
import io.gatehill.imposter.config.support.TestSupport
import io.gatehill.imposter.config.support.TestSupport.blockWait
import io.gatehill.imposter.config.support.TestSupport.uploadFileToS3
import io.gatehill.imposter.util.TestEnvironmentUtil.assumeDockerAccessible
import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import org.junit.*
import org.junit.Assert.assertTrue
import java.io.File
import java.nio.file.Files

/**
 * Verifies loading remote config.
 *
 * @author Pete Cornish
 */
class S3FileDownloaderTest {
    private var s3Mock: S3MockContainer? = null

    @Before
    fun setUp() {
        // These tests need Docker
        assumeDockerAccessible()

        s3Mock = TestSupport.startS3Mock()

        S3FileDownloader.destroyInstance()
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.name)
        System.setProperty(S3FileDownloader.SYS_PROP_S3_API_ENDPOINT, s3Mock!!.httpEndpoint)

        uploadFileToS3(s3Mock!!, "/config", "imposter-config.yaml")
        uploadFileToS3(s3Mock!!, "/config", "pet-api.yaml")
        uploadFileToS3(s3Mock!!, "/config", "subdir/response.json")

        // this directory should not be downloaded, but should be created locally
        uploadFileToS3(s3Mock!!, "/config", "unused-subdir/")
    }

    @After
    fun tearDown() {
        try {
            s3Mock?.takeIf { it.isRunning }?.stop()
        } catch (ignored: Exception) {
        }
        System.clearProperty(S3FileDownloader.SYS_PROP_S3_API_ENDPOINT)
        S3FileDownloader.destroyInstance()
    }

    /**
     * Should be able to load an OpenAPI specification from an S3 bucket.
     */
    @Test
    @Throws(Exception::class)
    fun testLoadConfigFromS3() {
        val configDir = Files.createTempDirectory("s3downloader").toFile()

        S3FileDownloader.getInstance().downloadAllFiles("s3://test", configDir)

        val configFile = File(configDir, "imposter-config.yaml")
        assertTrue("config should be fetched from S3", configFile.exists() && configFile.isFile)

        assertTrue("spec file should be fetched from S3", File(configDir, "pet-api.yaml").exists())
        assertTrue(
            "response file should be fetched from S3 subdir",
            File(configDir, "subdir/response.json").exists()
        )

        val localSubDir = File(configDir, "unused-subdir")
        assertTrue("local subdir should exist", localSubDir.exists())
        assertTrue("local subdir should be a directory", localSubDir.isDirectory)
    }

    companion object {
        private var vertx: Vertx? = null

        @JvmStatic
        @BeforeClass
        fun beforeClass() {
            vertx = Vertx.vertx()
        }

        @JvmStatic
        @AfterClass
        @Throws(Exception::class)
        fun afterClass() {
            blockWait<AsyncResult<Void>?> { completionHandler -> vertx!!.close(completionHandler) }
        }
    }
}
