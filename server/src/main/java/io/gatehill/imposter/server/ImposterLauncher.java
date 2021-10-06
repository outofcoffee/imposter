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

package io.gatehill.imposter.server;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.internal.MetaInfPluginDetectorImpl;
import io.gatehill.imposter.util.MetricsUtil;
import io.gatehill.imposter.util.FeatureUtil;
import io.gatehill.imposter.util.LogUtil;
import io.gatehill.imposter.util.MetaUtil;
import io.vertx.core.Launcher;
import io.vertx.core.VertxOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PASSWORD;
import static io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PATH;
import static io.gatehill.imposter.util.FileUtil.CLASSPATH_PREFIX;
import static io.gatehill.imposter.util.HttpUtil.BIND_ALL_HOSTS;
import static io.gatehill.imposter.util.HttpUtil.DEFAULT_HTTPS_LISTEN_PORT;
import static io.gatehill.imposter.util.HttpUtil.DEFAULT_HTTP_LISTEN_PORT;
import static io.gatehill.imposter.util.HttpUtil.DEFAULT_SERVER_FACTORY;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterLauncher extends Launcher {
    private static final Logger LOGGER = LogManager.getLogger(ImposterLauncher.class);
    private static final String VERTX_LOGGER_FACTORY = "vertx.logger-delegate-factory-class-name";
    private static final String VERTX_LOGGER_IMPL = "io.vertx.core.logging.SLF4JLogDelegateFactory";

    @Option(name = "--help", aliases = {"-h"}, usage = "Display usage only", help = true)
    private boolean displayHelp;

    @Option(name = "--version", aliases = {"-v"}, usage = "Print version and exit", help = true)
    private boolean displayVersion;

    @Option(name = "--configDir", aliases = {"-c"}, usage = "Directory containing mock configuration files", required = true)
    private String[] configDirs = {};

    @Option(name = "--plugin", aliases = {"-p"}, usage = "Plugin name (e.g. rest) or fully qualified class")
    private String[] plugins = {};

    @Option(name = "--listenPort", aliases = {"-l"}, usage = "Listen port (default " + DEFAULT_HTTP_LISTEN_PORT + " unless TLS enabled, in which case default is " + DEFAULT_HTTPS_LISTEN_PORT + ")")
    private Integer listenPort;

    @Option(name = "--host", aliases = {"-b"}, usage = "Bind host")
    private String host = BIND_ALL_HOSTS;

    @Option(name = "--serverUrl", aliases = {"-u"}, usage = "Explicitly set the server address")
    private String serverUrl;

    @Option(name = "--tlsEnabled", aliases = {"-t"}, usage = "Whether TLS (HTTPS) is enabled (requires keystore to be configured)")
    private boolean tlsEnabled;

    @Option(name = "--keystorePath", usage = "Path to the keystore")
    private String keystorePath = CLASSPATH_PREFIX + DEFAULT_KEYSTORE_PATH;

    @Option(name = "--keystorePassword", usage = "Password for the keystore")
    private String keystorePassword = DEFAULT_KEYSTORE_PASSWORD;

    @Option(name = "--pluginArg", usage = "Plugin arguments (key=value)")
    private String[] pluginArgs = {};

    @Option(name = "--serverFactory", usage = "Fully qualified class for server factory")
    private String serverFactory = DEFAULT_SERVER_FACTORY;

    static {
        // delegate all Vert.x logging to SLF4J
        System.setProperty(VERTX_LOGGER_FACTORY, VERTX_LOGGER_IMPL);
    }

    /**
     * Main entry point.
     *
     * @param args the user command line arguments.
     */
    public static void main(String[] args) {
        LogUtil.configureLoggingFromEnvironment();
        new ImposterLauncher().dispatch(args);
    }

    @Override
    public void beforeStartingVertx(VertxOptions options) {
        if (FeatureUtil.isFeatureEnabled(MetricsUtil.FEATURE_NAME_METRICS)) {
            MetricsUtil.configureMetrics(options);
        }
    }

    @Override
    public void dispatch(String[] originalArgs) {
        final CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(originalArgs);

            if (displayHelp) {
                printUsage(parser, 0);
            } else if (displayVersion) {
                printVersion();
            } else {
                startServer(originalArgs);
            }

        } catch (CmdLineException e) {
            // handling of wrong arguments
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(e);
            } else {
                LOGGER.error(e.getMessage());
            }

            printUsage(parser, 255);

        } catch (Exception e) {
            LOGGER.error("Error starting server", e);
        }
    }

    private void startServer(String[] originalArgs) {
        if (isNull(plugins) || 0 == plugins.length) {
            LOGGER.trace("Searching metadata for plugins");
            plugins = new String[]{MetaInfPluginDetectorImpl.class.getCanonicalName()};
        }

        final Map<String, String> splitArgs = Arrays.stream(ofNullable(pluginArgs).orElse(new String[0]))
                .map(arg -> arg.split("="))
                .collect(Collectors.toMap(splitArg -> splitArg[0], splitArg -> splitArg[1]));

        final int port;
        if (isNull(listenPort)) {
            if (tlsEnabled) {
                port = DEFAULT_HTTPS_LISTEN_PORT;
            } else {
                port = DEFAULT_HTTP_LISTEN_PORT;
            }
        } else {
            port = listenPort;
        }

        final ImposterConfig imposterConfig = ConfigHolder.getConfig();
        imposterConfig.setServerFactory(serverFactory);
        imposterConfig.setListenPort(port);
        imposterConfig.setHost(host);
        imposterConfig.setServerUrl(serverUrl);
        imposterConfig.setTlsEnabled(tlsEnabled);
        imposterConfig.setKeystorePath(keystorePath);
        imposterConfig.setKeystorePassword(keystorePassword);
        imposterConfig.setConfigDirs(configDirs);
        imposterConfig.setPlugins(plugins);
        imposterConfig.setPluginArgs(splitArgs);

        final List<String> args = newArrayList(originalArgs);
        args.add(0, "run");
        args.add(1, ImposterVerticle.class.getCanonicalName());
        super.dispatch(args.toArray(new String[0]));
    }

    private void printVersion() {
        System.out.println("Version: " + MetaUtil.readVersion());
    }

    /**
     * Print usage information, then exit.
     *
     * @param parser   the command line parser containing usage information
     * @param exitCode the exit code
     */
    private void printUsage(CmdLineParser parser, int exitCode) {
        System.out.println("Imposter: A scriptable, multipurpose mock server.");
        System.out.println("Usage:");
        parser.printUsage(System.out);
        System.exit(exitCode);
    }
}
