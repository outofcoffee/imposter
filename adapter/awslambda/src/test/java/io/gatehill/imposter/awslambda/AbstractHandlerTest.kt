/*
 * Copyright (c) 2016-2022.
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

package io.gatehill.imposter.awslambda

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.gatehill.imposter.config.S3FileDownloader
import io.gatehill.imposter.config.util.EnvVars
import io.gatehill.imposter.util.TestEnvironmentUtil.assumeDockerAccessible
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import java.nio.file.Paths

abstract class AbstractHandlerTest {
    companion object {
        private var s3Mock: S3MockContainer? = null

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            // These tests need Docker
            assumeDockerAccessible()

            s3Mock = S3MockContainer("2.2.1").apply {
                withInitialBuckets("test")
                start()
            }

            S3FileDownloader.destroyInstance()
            System.setProperty(S3FileDownloader.SYS_PROP_S3_API_ENDPOINT, s3Mock!!.httpEndpoint)

            uploadFileToS3("/config", "imposter-config.yaml")
            uploadFileToS3("/config", "pet-api.yaml")
            uploadFileToS3("/config", "subdir/response.json")

            EnvVars.populate(
                "IMPOSTER_S3_CONFIG_URL" to "s3://test/",
            )
        }

        private fun uploadFileToS3(baseDir: String, filePath: String) {
            val specFilePath = Paths.get(AbstractHandlerTest::class.java.getResource("$baseDir/$filePath")!!.toURI())

            val s3 = AmazonS3ClientBuilder.standard()
                .enablePathStyleAccess()
                .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(s3Mock!!.httpEndpoint, "us-east-1"))
                .build()

            s3.putObject("test", filePath, specFilePath.toFile())
        }

        @AfterAll
        @JvmStatic
        fun afterAll() {
            try {
                s3Mock?.takeIf { it.isRunning }?.stop()
            } catch (ignored: Exception) {
            }
            System.clearProperty(S3FileDownloader.SYS_PROP_S3_API_ENDPOINT)
            S3FileDownloader.destroyInstance()
        }
    }
}
