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

package io.gatehill.imposter.embedded;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.Plugin;
import io.gatehill.imposter.script.listener.ScriptListener;
import io.gatehill.imposter.server.ConfigHolder;
import io.gatehill.imposter.server.ImposterVerticle;
import io.gatehill.imposter.server.vertxweb.VertxWebServerFactoryImpl;
import io.gatehill.imposter.service.script.EmbeddedScriptService;
import io.gatehill.imposter.util.FeatureUtil;
import io.gatehill.imposter.util.InjectorUtil;
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
import java.util.function.Consumer;

import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
 * @author Pete Cornish
 * @see io.gatehill.imposter.openapi.embedded.OpenApiImposterBuilder
 */
public class ImposterBuilder<M extends MockEngine, SELF extends ImposterBuilder<M, SELF>> {
    protected static final Logger LOGGER = LogManager.getLogger(ImposterBuilder.class);
    static final String HOST = "localhost";

    private final Vertx vertx = Vertx.vertx();
    protected final List<Path> configurationDirs = new ArrayList<>();
    protected Class<? extends Plugin> pluginClass;
    private ScriptListener scriptListener;
    private Consumer<ImposterConfig> optionsListener;

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

    public SELF withScriptedBehaviour(ScriptListener scriptListener) {
        this.scriptListener = scriptListener;
        return self();
    }

    public SELF withEngineOptions(Consumer<ImposterConfig> optionsListener) {
        this.optionsListener = optionsListener;
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
                configureScriptEngine();
                future.complete(mockEngine);
            } else {
                future.completeExceptionally(completion.cause());
            }
        });
    }

    private void configureScriptEngine() {
        if (isNull(scriptListener)) {
            return;
        }
        final EmbeddedScriptService embeddedScriptService = InjectorUtil.getInjector().getInstance(
                EmbeddedScriptService.class
        );
        embeddedScriptService.setListener(scriptListener);
    }

    @SuppressWarnings("unchecked")
    protected M buildEngine(ImposterConfig config) {
        return (M) new MockEngine(config);
    }

    private void configure(ImposterConfig imposterConfig, int port) {
        imposterConfig.setServerFactory(VertxWebServerFactoryImpl.class.getCanonicalName());
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

        if (nonNull(scriptListener)) {
            imposterConfig.setUseEmbeddedScriptEngine(true);
        }
        if (nonNull(optionsListener)) {
            optionsListener.accept(imposterConfig);
        }
    }

    private int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Unable to find a free port");
        }
    }
}
