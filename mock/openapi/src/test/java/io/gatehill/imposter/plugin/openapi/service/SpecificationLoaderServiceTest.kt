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
package io.gatehill.imposter.plugin.openapi.service

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import com.amazonaws.SDKGlobalConfiguration
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.gatehill.imposter.config.S3FileDownloader
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig
import io.gatehill.imposter.service.FileCacheService
import io.gatehill.imposter.util.TestEnvironmentUtil.assumeDockerAccessible
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.http.HttpServerRequest
import org.junit.*
import java.net.ServerSocket
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer

/**
 * Verifies loading local and remote specifications.
 *
 * @author Pete Cornish
 */
class SpecificationLoaderServiceTest {
    private val noOpFileCacheService = object : FileCacheService {
        override fun readFromCache(cacheKey: String) = FileCacheService.CacheResult(false)
        override fun writeToCache(cacheKey: String, content: String) {}
    }
    private val service = SpecificationLoaderService(noOpFileCacheService)
    private var s3Mock: S3MockContainer? = null

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
     * Should be able to load an OpenAPI specification from a local file.
     */
    @Test
    @Throws(Exception::class)
    fun testLoadSpecificationFromFile() {
        val specFilePath =
            Paths.get(
                SpecificationLoaderServiceTest::class.java.getResource("/util/spec-loader/order_service.yaml")!!.toURI()
            )

        val pluginConfig = OpenApiPluginConfig()
        pluginConfig.dir = specFilePath.parent.toFile()
        pluginConfig.specFile = specFilePath.fileName.toString()

        val spec = service.parseSpecification(pluginConfig)
        Assert.assertNotNull("spec should be loaded from file", spec)
        Assert.assertEquals("title should match", "Sample Petstore order service", spec.info.title)
    }

    /**
     * Should be able to load an OpenAPI specification from a URL.
     */
    @Test
    @Throws(Exception::class)
    fun testLoadSpecificationFromUrl() {
        val listenPort = ServerSocket(0).use { it.localPort }
        val specFilePath =
            Paths.get(
                SpecificationLoaderServiceTest::class.java.getResource("/util/spec-loader/order_service.yaml")!!.toURI()
            )

        val httpServer = vertx!!.createHttpServer(HttpServerOptions().setPort(listenPort))
        httpServer.requestHandler { request: HttpServerRequest -> request.response().sendFile(specFilePath.toString()) }
        blockWait { listenHandler: Handler<AsyncResult<HttpServer?>?>? -> httpServer.listen(listenHandler) }

        val pluginConfig = OpenApiPluginConfig()
        pluginConfig.dir = specFilePath.parent.toFile()
        pluginConfig.specFile = "http://localhost:$listenPort"

        val spec = service.parseSpecification(pluginConfig)
        Assert.assertNotNull("spec should be loaded from URL", spec)
        Assert.assertEquals("title should match", "Sample Petstore order service", spec.info.title)
    }

    /**
     * Should be able to load an OpenAPI specification from an S3 bucket.
     */
    @Test
    @Throws(Exception::class)
    fun testLoadSpecificationFromS3() {
        // These tests need Docker
        assumeDockerAccessible()

        s3Mock = S3MockContainer("2.4.10").apply {
            withInitialBuckets("test")
            start()
        }

        S3FileDownloader.destroyInstance()
        System.setProperty(SDKGlobalConfiguration.AWS_REGION_SYSTEM_PROPERTY, Regions.US_EAST_1.name)
        System.setProperty(S3FileDownloader.SYS_PROP_S3_API_ENDPOINT, s3Mock!!.httpEndpoint)

        val specFilePath =
            Paths.get(
                SpecificationLoaderServiceTest::class.java.getResource("/util/spec-loader/order_service.yaml")!!.toURI()
            )
        val pluginConfig = uploadFileToS3(specFilePath)

        val spec = service.parseSpecification(pluginConfig)
        Assert.assertNotNull("spec should be loaded from S3", spec)
        Assert.assertEquals("title should match", "Sample Petstore order service", spec.info.title)
    }

    private fun uploadFileToS3(specFilePath: Path): OpenApiPluginConfig {
        val s3 = AmazonS3ClientBuilder.standard()
            .enablePathStyleAccess()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(s3Mock!!.httpEndpoint, "us-east-1"))
            .build()

        s3.putObject("test", "order_service.yaml", specFilePath.toFile())

        val pluginConfig = OpenApiPluginConfig()
        pluginConfig.dir = specFilePath.parent.toFile()
        pluginConfig.specFile = "s3://test/order_service.yaml"
        return pluginConfig
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
            vertx?.close()
        }

        /**
         * Block the consumer until the handler is called.
         *
         * @param handlerConsumer the consumer of the handler
         * @param <T>             the type of the async result
         */
        @Throws(Exception::class)
        private fun <T> blockWait(handlerConsumer: Consumer<Handler<T>>) {
            val latch = CountDownLatch(1)
            val handler = Handler { _: T -> latch.countDown() }
            handlerConsumer.accept(handler)
            latch.await()
        }
    }
}