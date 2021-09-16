package io.gatehill.imposter.embedded;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.server.ConfigHolder;
import io.gatehill.imposter.server.ImposterVerticle;
import io.gatehill.imposter.util.FeatureUtil;
import io.gatehill.imposter.util.MetricsUtil;
import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static io.gatehill.imposter.util.HttpUtil.DEFAULT_SERVER_FACTORY;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;

/**
 * Convenience class for building Imposter mock engine instances.
 * <p>
 * Example using a directory containing an Imposter configuration file:
 * <pre>
 * MockEngine imposter = new ImposterBuilder<>()
 *             .withPluginClass(OpenApiPluginImpl.class)
 *             .withConfigurationDir(configDir)
 *             .startBlocking();
 *
 * // mockEndpoint will look like http://localhost:5234/v1/pets
 * String mockEndpoint = imposter.getBaseUrl() + "/v1/pets";
 *
 * // Your component under test can interact with this endpoint to get
 * // simulated HTTP responses, in place of a real endpoint.
 * </pre>
 * Note the need to specify the plugin.
 * <p>
 * Typically, you will want to use a plugin-specific builder if it exists,
 * such as {@link io.gatehill.imposter.openapi.embedded.OpenApiImposterBuilder}
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 * @see io.gatehill.imposter.openapi.embedded.OpenApiImposterBuilder
 */
public class ImposterBuilder<M extends MockEngine, SELF extends ImposterBuilder<M, SELF>> {
    protected static final Logger LOGGER = LogManager.getLogger(ImposterBuilder.class);
    static final String HOST = "localhost";

    private final Vertx vertx = Vertx.vertx();
    protected final List<Path> configurationDirs = new ArrayList<>();
    protected Class<? extends Plugin> pluginClass;

    @SuppressWarnings("unchecked")
    protected SELF self() {
        return (SELF) this;
    }

    /**
     * The plugin to use.
     *
     * @param pluginClass the plugin
     */
    public SELF withPluginClass(Class<? extends Plugin> pluginClass) {
        this.pluginClass = pluginClass;
        return self();
    }

    /**
     * The directory containing a valid Imposter configuration file.
     *
     * @param configurationDir the directory
     */
    public SELF withConfigurationDir(String configurationDir) {
        return withConfigurationDir(Paths.get(configurationDir));
    }

    /**
     * The directory containing a valid Imposter configuration file.
     *
     * @param configurationDir the directory
     */
    public SELF withConfigurationDir(Path configurationDir) {
        this.configurationDirs.add(configurationDir);
        return self();
    }

    public CompletableFuture<M> startAsync() {
        final CompletableFuture<M> future = new CompletableFuture<>();
        try {
            if (configurationDirs.isEmpty()) {
                throw new IllegalStateException("Must specify one of specification file or specification directory");
            }
            if (isNull(pluginClass)) {
                throw new IllegalStateException("Must specify plugin class implementing " + Plugin.class.getCanonicalName());
            }
            bootMockEngine(future);

        } catch (Exception e) {
            throw new ImposterLaunchException("Error starting Imposter mock engine", e);
        }
        return future;
    }

    public M startBlocking() {
        try {
            final CompletableFuture<M> future = startAsync();
            LOGGER.debug("Waiting for mock engine to start...");
            return future.get();
        } catch (Exception e) {
            throw new ImposterLaunchException(e);
        }
    }

    private void bootMockEngine(CompletableFuture<M> future) {
        FeatureUtil.disableFeature(MetricsUtil.FEATURE_NAME_METRICS);

        final int port = findFreePort();

        ConfigHolder.resetConfig();
        final ImposterConfig config = ConfigHolder.getConfig();
        configure(config, port);

        final M mockEngine = buildEngine(config);

        // wait for the engine to parse and combine the specifications
        vertx.deployVerticle(ImposterVerticle.class.getCanonicalName(), completion -> {
            if (completion.succeeded()) {
                mockEngine.logStartup();
                future.complete(mockEngine);
            } else {
                future.completeExceptionally(completion.cause());
            }
        });
    }

    @SuppressWarnings("unchecked")
    protected M buildEngine(ImposterConfig config) {
        return (M) new MockEngine(config);
    }

    private void configure(ImposterConfig imposterConfig, int port) {
        imposterConfig.setServerFactory(DEFAULT_SERVER_FACTORY);
        imposterConfig.setHost(HOST);
        imposterConfig.setListenPort(port);
        imposterConfig.setPlugins(new String[]{pluginClass.getCanonicalName()});
        imposterConfig.setPluginArgs(emptyMap());

        imposterConfig.setConfigDirs(configurationDirs.stream().map(dir -> {
            try {
                return dir.toString();
            } catch (Exception e) {
                throw new RuntimeException("Error parsing directory: " + dir, e);
            }
        }).toArray(String[]::new));
    }

    private int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Unable to find a free port");
        }
    }
}
