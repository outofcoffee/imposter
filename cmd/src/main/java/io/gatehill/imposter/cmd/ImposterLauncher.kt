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
package io.gatehill.imposter.cmd

import io.gatehill.imposter.config.util.MetaUtil.readVersion
import io.gatehill.imposter.plugin.DynamicPluginDiscoveryStrategyImpl
import io.gatehill.imposter.plugin.internal.MetaInfPluginDetectorImpl
import io.gatehill.imposter.server.ConfigHolder
import io.gatehill.imposter.server.vertxweb.VertxWebServerFactoryImpl
import io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PASSWORD
import io.gatehill.imposter.util.CryptoUtil.DEFAULT_KEYSTORE_PATH
import io.gatehill.imposter.util.FileUtil.CLASSPATH_PREFIX
import io.gatehill.imposter.util.HttpUtil.BIND_ALL_HOSTS
import io.gatehill.imposter.util.HttpUtil.DEFAULT_HTTPS_LISTEN_PORT
import io.gatehill.imposter.util.HttpUtil.DEFAULT_HTTP_LISTEN_PORT
import io.gatehill.imposter.util.LogUtil
import org.apache.logging.log4j.LogManager
import org.kohsuke.args4j.CmdLineException
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import kotlin.system.exitProcess

/**
 * @author Pete Cornish
 */
class ImposterLauncher(args: Array<String>) {
    @Option(name = "--help", aliases = ["-h"], usage = "Display usage only", help = true)
    private var displayHelp = false

    @Option(name = "--version", aliases = ["-v"], usage = "Print version and exit", help = true)
    private var displayVersion = false

    @Option(
        name = "--configDir",
        aliases = ["-c"],
        usage = "Directory containing mock configuration files",
        required = true
    )
    private var configDirs = arrayOf<String>()

    @Option(name = "--plugin", aliases = ["-p"], usage = "Plugin name (e.g. rest) or fully qualified class")
    private var plugins = arrayOf<String>()

    @Option(
        name = "--listenPort",
        aliases = ["-l"],
        usage = "Listen port (default $DEFAULT_HTTP_LISTEN_PORT unless TLS enabled, in which case default is $DEFAULT_HTTPS_LISTEN_PORT)"
    )
    private var listenPort: Int? = null

    @Option(name = "--host", aliases = ["-b"], usage = "Bind host")
    private var host: String = BIND_ALL_HOSTS

    @Option(name = "--serverUrl", aliases = ["-u"], usage = "Explicitly set the server address")
    private var serverUrl: String? = null

    @Option(
        name = "--tlsEnabled",
        aliases = ["-t"],
        usage = "Whether TLS (HTTPS) is enabled (requires keystore to be configured)"
    )
    private var tlsEnabled = false

    @Option(name = "--keystorePath", usage = "Path to the keystore")
    private var keystorePath: String = CLASSPATH_PREFIX + DEFAULT_KEYSTORE_PATH

    @Option(name = "--keystorePassword", usage = "Password for the keystore")
    private var keystorePassword: String = DEFAULT_KEYSTORE_PASSWORD

    @Option(name = "--pluginArg", usage = "Plugin arguments (key=value)")
    private var pluginArgs = arrayOf<String>()

    @Option(name = "--serverFactory", usage = "Fully qualified class for server factory")
    private var serverFactory: String = VertxWebServerFactoryImpl::class.java.canonicalName

    companion object {
        private val LOGGER = LogManager.getLogger(ImposterLauncher::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            LogUtil.configureLoggingFromEnvironment()
            LogUtil.configureVertxLogging()
            ImposterLauncher(args)
        }
    }

    init {
        val parser = CmdLineParser(this)
        try {
            parser.parseArgument(*args)
            if (displayHelp) {
                printUsage(parser, 0)
            } else if (displayVersion) {
                printVersion()
            } else {
                startServer(args)
            }
        } catch (e: CmdLineException) {
            if (LOGGER.isDebugEnabled) {
                LOGGER.debug(e)
            } else {
                LOGGER.error(e.message)
            }
            printUsage(parser, 255)
        } catch (e: Exception) {
            LOGGER.error("Error starting server", e)
        }
    }

    /**
     * Print usage information, then exit.
     *
     * @param parser   the command line parser containing usage information
     * @param exitCode the exit code
     */
    private fun printUsage(parser: CmdLineParser, exitCode: Int) {
        println("Imposter: A scriptable, multipurpose mock server.")
        println("Usage:")
        parser.printUsage(System.out)
        exitProcess(exitCode)
    }

    private fun printVersion() {
        println("Version: ${readVersion()}")
    }

    private fun startServer(originalArgs: Array<String>) {
        if (plugins.isEmpty()) {
            LOGGER.trace("Searching metadata for plugins")
            plugins = arrayOf(MetaInfPluginDetectorImpl::class.java.canonicalName)
        }

        val splitArgs = pluginArgs.map { arg: String ->
            arg.split("=").toTypedArray()
        }.associate {
            it[0] to it[1]
        }

        val port = listenPort ?: run {
            if (tlsEnabled) DEFAULT_HTTPS_LISTEN_PORT else DEFAULT_HTTP_LISTEN_PORT
        }

        if (configDirs.isEmpty()) {
            LOGGER.error("No configuration directories were provided")
            exitProcess(0)
        }

        ConfigHolder.config.let { imposterConfig ->
            imposterConfig.serverFactory = serverFactory
            imposterConfig.pluginDiscoveryStrategy = DynamicPluginDiscoveryStrategyImpl::class.qualifiedName
            imposterConfig.listenPort = port
            imposterConfig.host = host
            imposterConfig.serverUrl = serverUrl
            imposterConfig.isTlsEnabled = tlsEnabled
            imposterConfig.keystorePath = keystorePath
            imposterConfig.keystorePassword = keystorePassword
            imposterConfig.configDirs = configDirs
            imposterConfig.plugins = plugins
            imposterConfig.pluginArgs = splitArgs
        }

        LifecycleAwareLauncher().dispatch(originalArgs)
    }
}
