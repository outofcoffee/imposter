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

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.tests.annotations.Event
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.gatehill.imposter.awslambda.Handler
import io.gatehill.imposter.plugin.openapi.loader.S3FileDownloader
import io.gatehill.imposter.util.EnvVars
import io.gatehill.imposter.util.TestEnvironmentUtil.assumeDockerAccessible
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.mockito.Mockito.mock
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

class HandlerTest {
    private var s3Mock: S3MockContainer? = null
    private var handler: Handler? = null
    private var context: Context? = null

    @BeforeEach
    fun setUp() {
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

        val configDir = Files.createTempDirectory("imposter-config")
        EnvVars.populate(mapOf(
            "IMPOSTER_CONFIG_DIR" to configDir.absolutePathString(),
            "IMPOSTER_S3_CONFIG_URL" to "s3://test/",
        ))

        handler = Handler()
        context = mock(Context::class.java)
    }

    private fun uploadFileToS3(baseDir: String, filePath: String) {
        val specFilePath = Paths.get(HandlerTest::class.java.getResource("$baseDir/$filePath")!!.toURI())

        val s3 = AmazonS3ClientBuilder.standard()
            .enablePathStyleAccess()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(s3Mock!!.httpEndpoint, "us-east-1"))
            .build()

        s3.putObject("test", filePath, specFilePath.toFile())
    }

    @AfterEach
    fun tearDown() {
        try {
            s3Mock?.takeIf { it.isRunning }?.stop()
        } catch (ignored: Exception) {
        }
        System.clearProperty(S3FileDownloader.SYS_PROP_S3_API_ENDPOINT)
        S3FileDownloader.destroyInstance()
    }

    @ParameterizedTest
    @Event(value = "requests/request_spec_example.json", type = APIGatewayProxyRequestEvent::class)
    fun `get example from spec`(event: APIGatewayProxyRequestEvent) {
        val responseEvent = handler!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(200, responseEvent.statusCode)
        assertEquals("""{ "id": 1, "name": "Cat" }""", responseEvent.body)
        assertEquals(4, responseEvent.headers?.size)
        assertEquals("imposter", responseEvent.headers["Server"])
    }

    @ParameterizedTest
    @Event(value = "requests/request_static_file.json", type = APIGatewayProxyRequestEvent::class)
    fun `get static file`(event: APIGatewayProxyRequestEvent) {
        val responseEvent = handler!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(200, responseEvent.statusCode)
        assertEquals("""{ "id": 2, "name": "Dog" }""", responseEvent.body)
        assertEquals(4, responseEvent.headers?.size)
        assertEquals("imposter", responseEvent.headers["Server"])
    }

    @ParameterizedTest
    @Event(value = "requests/request_no_route.json", type = APIGatewayProxyRequestEvent::class)
    fun `no matching route`(event: APIGatewayProxyRequestEvent) {
        val responseEvent = handler!!.handleRequest(event, context!!)

        assertNotNull(responseEvent, "Response event should be returned")
        assertEquals(404, responseEvent.statusCode)
        assertEquals("Resource not found", responseEvent.body)
        assertEquals(2, responseEvent.headers?.size)
        assertEquals("text/plain", responseEvent.headers["Content-Type"])
    }
}
