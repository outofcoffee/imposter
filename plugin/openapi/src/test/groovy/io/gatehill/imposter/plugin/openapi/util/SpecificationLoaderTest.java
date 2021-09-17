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

package io.gatehill.imposter.plugin.openapi.util;

import com.adobe.testing.s3mock.testcontainers.S3MockContainer;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.gatehill.imposter.plugin.openapi.loader.S3SpecificationLoader;
import io.gatehill.imposter.plugin.openapi.loader.SpecificationLoader;
import io.gatehill.imposter.util.TestEnvironmentUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static java.util.Objects.nonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies loading local and remote specifications.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SpecificationLoaderTest {
    private static Vertx vertx;
    private S3MockContainer s3Mock;

    @BeforeClass
    public static void beforeClass() {
        vertx = Vertx.vertx();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SpecificationLoaderTest.<AsyncResult<Void>>blockWait(vertx::close);
    }

    @After
    public void tearDown() {
        try {
            if (nonNull(s3Mock) && s3Mock.isRunning()) {
                s3Mock.stop();
            }
        } catch (Exception ignored) {
        }

        System.clearProperty(S3SpecificationLoader.SYS_PROP_OPENAPI_S3_API_ENDPOINT);
        S3SpecificationLoader.destroyInstance();
    }

    /**
     * Should be able to load an OpenAPI specification from a local file.
     */
    @Test
    public void testLoadSpecificationFromFile() throws Exception {
        final Path specFilePath = Paths.get(SpecificationLoaderTest.class.getResource("/util/spec-loader/order_service.yaml").toURI());

        final OpenApiPluginConfig pluginConfig = new OpenApiPluginConfig();
        pluginConfig.setParentDir(specFilePath.getParent().toFile());
        pluginConfig.setSpecFile(specFilePath.getFileName().toString());
        final OpenAPI spec = SpecificationLoader.parseSpecification(pluginConfig);

        assertNotNull("spec should be loaded from file", spec);
        assertEquals("title should match", "Sample Petstore order service", spec.getInfo().getTitle());
    }

    /**
     * Should be able to load an OpenAPI specification from a URL.
     */
    @Test
    public void testLoadSpecificationFromUrl() throws Exception {
        final int listenPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            listenPort = socket.getLocalPort();
        }

        final Path specFilePath = Paths.get(SpecificationLoaderTest.class.getResource("/util/spec-loader/order_service.yaml").toURI());
        final HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(listenPort));
        httpServer.requestHandler(request -> request.response().sendFile(specFilePath.toString()));

        SpecificationLoaderTest.<AsyncResult<HttpServer>>blockWait(httpServer::listen);

        final OpenApiPluginConfig pluginConfig = new OpenApiPluginConfig();
        pluginConfig.setParentDir(specFilePath.getParent().toFile());
        pluginConfig.setSpecFile("http://localhost:" + listenPort);
        final OpenAPI spec = SpecificationLoader.parseSpecification(pluginConfig);

        assertNotNull("spec should be loaded from URL", spec);
        assertEquals("title should match", "Sample Petstore order service", spec.getInfo().getTitle());
    }

    /**
     * Should be able to load an OpenAPI specification from an S3 bucket.
     */
    @Test
    public void testLoadSpecificationFromS3() throws Exception {
        // Testcontainers hangs in CircleCI
        TestEnvironmentUtil.assumeNotInCircleCi();

        // These tests need Docker
        TestEnvironmentUtil.assumeDockerAccessible();

        final Path specFilePath = Paths.get(SpecificationLoaderTest.class.getResource("/util/spec-loader/order_service.yaml").toURI());

        s3Mock = new S3MockContainer("2.2.1");
        s3Mock.withInitialBuckets("test");
        s3Mock.start();

        System.setProperty(S3SpecificationLoader.SYS_PROP_OPENAPI_S3_API_ENDPOINT, s3Mock.getHttpEndpoint());
        S3SpecificationLoader.destroyInstance();

        final AmazonS3 s3 = AmazonS3ClientBuilder.standard()
                .enablePathStyleAccess()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s3Mock.getHttpEndpoint(), "us-east-1"))
                .build();
        s3.putObject("test", "order_service.yaml", specFilePath.toFile());

        final OpenApiPluginConfig pluginConfig = new OpenApiPluginConfig();
        pluginConfig.setParentDir(specFilePath.getParent().toFile());
        pluginConfig.setSpecFile("s3://test/order_service.yaml");
        final OpenAPI spec = SpecificationLoader.parseSpecification(pluginConfig);

        assertNotNull("spec should be loaded from S3", spec);
        assertEquals("title should match", "Sample Petstore order service", spec.getInfo().getTitle());
    }

    /**
     * Block the consumer until the handler is called.
     *
     * @param handlerConsumer the consumer of the handler
     * @param <T>             the type of the async result
     */
    private static <T> void blockWait(Consumer<Handler<T>> handlerConsumer) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final Handler<T> handler = event -> latch.countDown();
        handlerConsumer.accept(handler);
        latch.await();
    }
}
