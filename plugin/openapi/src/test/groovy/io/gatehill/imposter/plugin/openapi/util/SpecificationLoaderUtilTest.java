package io.gatehill.imposter.plugin.openapi.util;

import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies loading local and remote specifications.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SpecificationLoaderUtilTest {
    private static Vertx vertx;

    @BeforeClass
    public static void beforeClass() {
        vertx = Vertx.vertx();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        SpecificationLoaderUtilTest.<AsyncResult<Void>>blockWait(vertx::close);
    }

    /**
     * Should be able to load an OpenAPI specification from a local file.
     */
    @Test
    public void testLoadLocalSpecification() throws Exception {
        final Path specFilePath = Paths.get(SpecificationLoaderUtilTest.class.getResource("/util/spec-loader/order_service.yaml").toURI());

        final OpenApiPluginConfig pluginConfig = new OpenApiPluginConfig();
        pluginConfig.setParentDir(specFilePath.getParent().toFile());
        pluginConfig.setSpecFile(specFilePath.getFileName().toString());
        final OpenAPI spec = SpecificationLoaderUtil.parseSpecification(pluginConfig);

        assertNotNull("Local OpenAPI spec should be loaded", spec);
        assertEquals("title should match", "Sample Petstore order service", spec.getInfo().getTitle());
    }

    /**
     * Should be able to load an OpenAPI specification from a URL.
     */
    @Test
    public void testLoadRemoteSpecification() throws Exception {
        final int listenPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            listenPort = socket.getLocalPort();
        }

        final Path specFilePath = Paths.get(SpecificationLoaderUtilTest.class.getResource("/util/spec-loader/order_service.yaml").toURI());
        final HttpServer httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(listenPort));
        httpServer.requestHandler(request -> request.response().sendFile(specFilePath.toString()));

        SpecificationLoaderUtilTest.<AsyncResult<HttpServer>>blockWait(httpServer::listen);

        final OpenApiPluginConfig pluginConfig = new OpenApiPluginConfig();
        pluginConfig.setParentDir(specFilePath.getParent().toFile());
        pluginConfig.setSpecFile("http://localhost:" + listenPort);
        final OpenAPI spec = SpecificationLoaderUtil.parseSpecification(pluginConfig);

        assertNotNull("Remote OpenAPI spec should be loaded", spec);
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
