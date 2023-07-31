package io.gatehill.imposter.config.support

import com.adobe.testing.s3mock.testcontainers.S3MockContainer
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.gatehill.imposter.config.S3FileDownloaderTest
import io.vertx.core.Handler
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.function.Consumer

object TestSupport {
    fun startS3Mock(): S3MockContainer {
        return S3MockContainer("2.4.10").apply {
            withInitialBuckets("test")
            start()
        }
    }

    fun uploadFileToS3(s3Mock: S3MockContainer, baseDir: String, filePath: String) {
        val s3 = AmazonS3ClientBuilder.standard()
            .enablePathStyleAccess()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(s3Mock.httpEndpoint, "us-east-1"))
            .build()

        if (filePath.endsWith("/")){
            s3.putObject("test", filePath, "")
        } else {
            val specFilePath = Paths.get(S3FileDownloaderTest::class.java.getResource("$baseDir/$filePath")!!.toURI())
            s3.putObject("test", filePath, specFilePath.toFile())
        }
    }

    /**
     * Block the consumer until the handler is called.
     *
     * @param handlerConsumer the consumer of the handler
     * @param <T>             the type of the async result
     */
    @Throws(Exception::class)
    fun <T> blockWait(handlerConsumer: Consumer<Handler<T>>) {
        val latch = CountDownLatch(1)
        val handler = Handler { _: T -> latch.countDown() }
        handlerConsumer.accept(handler)
        latch.await()
    }
}