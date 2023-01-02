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

package io.gatehill.imposter.plugin

import com.google.inject.Module
import io.gatehill.imposter.ImposterConfig
import io.gatehill.imposter.config.util.ConfigUtil
import io.gatehill.imposter.util.ClassLoaderUtil
import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import org.apache.logging.log4j.LogManager
import java.io.File
import java.util.*

/**
 * Scans the classpath to find classes implementing [Plugin] and [PluginProvider],
 * instantiating any plugin [Module]s on demand.
 *
 * Each plugin provider is called recursively to discover all plugins.
 *
 * @see StaticPluginDiscoveryStrategyImpl
 */
class DynamicPluginDiscoveryStrategyImpl : PluginDiscoveryStrategy {
    private val classpathPlugins = mutableMapOf<String, String>()
    private val finalPluginClasses = mutableSetOf<Class<out Plugin>>()
    private var hasScannedForPlugins = false

    /**
     * {@inheritDoc}
     */
    override fun preparePluginsFromConfig(
        imposterConfig: ImposterConfig,
        initialPlugins: List<String>,
        pluginConfigs: Map<String, List<File>>
    ): List<PluginDependencies> {
        val plugins = initialPlugins.toMutableList()
        val providers = mutableListOf<Class<PluginProvider>>()

        val registeredPluginClasses = mutableSetOf<Class<out Plugin>>()
        val registeredProviders = mutableSetOf<Class<out PluginProvider>>()
        val dependencies = mutableListOf<PluginDependencies>()

        while (plugins.isNotEmpty()) {
            val plugin = plugins.first()
            plugins -= plugin
            processPlugin(providers, registeredPluginClasses, dependencies, plugin)

            while (providers.isNotEmpty()) {
                val provider = providers.first()
                providers -= provider
                processProvider(imposterConfig, plugins, pluginConfigs, registeredProviders, provider)
            }
        }

        finalPluginClasses += registeredPluginClasses

        return dependencies
    }

    private fun processPlugin(
        providers: MutableList<Class<PluginProvider>>,
        pluginClasses: MutableSet<Class<out Plugin>>,
        dependencies: MutableList<PluginDependencies>,
        nextPlugin: String,
    ) {
        val pluginClassname = determinePluginClass(nextPlugin)
        val pluginClass = registerPluginClass(pluginClasses, nextPlugin, pluginClassname)

        pluginClass?.let {
            dependencies += examinePlugin(pluginClass)
            if (PluginProvider::class.java.isAssignableFrom(pluginClass)) {
                @Suppress("UNCHECKED_CAST")
                val providerClass = pluginClass.asSubclass(PluginProvider::class.java) as Class<PluginProvider>
                providers += providerClass
            }
        }
    }

    private fun registerPluginClass(
        pluginClasses: MutableSet<Class<out Plugin>>,
        pluginName: String,
        pluginClassname: String,
    ): Class<Plugin>? {
        try {
            val clazz = ClassLoaderUtil.loadClass<Plugin>(pluginClassname)
            val registered = pluginClasses.add(clazz)
            if (registered) {
                logger.trace("Registered plugin: $pluginName with class: $pluginClassname")
                return clazz
            } else {
                logger.warn("Skipped registering existing plugin: $pluginName with class: $pluginClassname")
                return null
            }
        } catch (e: ClassNotFoundException) {
            logger.trace(e)
            throw RuntimeException("Could not find the plugin '$pluginClassname' - is it installed in the plugin path (${ClassLoaderUtil.describePluginPath()})?")
        } catch (e: Exception) {
            throw RuntimeException("Failed to register plugin: $pluginClassname", e)
        }
    }

    private fun processProvider(
        imposterConfig: ImposterConfig,
        plugins: MutableList<String>,
        pluginConfigs: Map<String, List<File>>,
        registeredProviders: MutableSet<Class<out PluginProvider>>,
        providerClass: Class<PluginProvider>,
    ) {
        registeredProviders += providerClass

        val pluginProvider = createPluginProvider(providerClass)
        val provided = pluginProvider.providePlugins(imposterConfig, pluginConfigs)
        if (logger.isTraceEnabled) {
            logger.trace("${provided.size} plugin(s) provided by: ${getPluginNameFromAnnotation(providerClass)}")
        }
        plugins += provided
    }

    private fun createPluginProvider(providerClass: Class<PluginProvider>): PluginProvider {
        return try {
            providerClass.getDeclaredConstructor().newInstance()
        } catch (e: Exception) {
            throw RuntimeException("Error instantiating plugin provider: ${providerClass.canonicalName}", e)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun determinePluginClass(plugin: String): String {
        if (!hasScannedForPlugins) {
            synchronized(classpathPlugins) {
                if (!hasScannedForPlugins) { // double-guard
                    classpathPlugins.putAll(discoverClasspathPlugins())
                    hasScannedForPlugins = true
                }
            }
        }
        val normalisedPluginId = plugin.removeSurrounding("\"")
        return classpathPlugins[normalisedPluginId] ?: normalisedPluginId
    }

    override fun getPluginClasses(): Collection<Class<out Plugin>> {
        return Collections.unmodifiableCollection(finalPluginClasses)
    }

    override fun getPluginName(clazz: Class<in Plugin>): String {
        return getPluginNameFromAnnotation(clazz)
    }

    private fun getPluginNameFromAnnotation(clazz: Class<*>): String {
        return clazz.getAnnotation(PluginInfo::class.java)?.value ?: clazz.canonicalName
    }

    /**
     * Finds plugins on the classpath annotated with [PluginInfo].
     *
     * @return a map of plugin short names to full qualified class names
     */
    private fun discoverClasspathPlugins(): Map<String, String> {
        val startMs = System.currentTimeMillis()
        val pluginClasses = mutableMapOf<String, String>()

        ClassGraph().enableClassInfo().enableAnnotationInfo()
            .addClassLoader(ClassLoaderUtil.pluginClassLoader)
            .acceptPackages(*pluginBasePackages).scan().use { result ->
                val pluginClassInfos = result
                    .getClassesImplementing(Plugin::class.qualifiedName)
                    .filter { classInfo: ClassInfo -> classInfo.hasAnnotation(PluginInfo::class.qualifiedName) }

                for (pluginClassInfo in pluginClassInfos) {
                    try {
                        val pluginName = pluginClassInfo.annotationInfo[0].parameterValues[0].value as String
                        pluginClasses[pluginName] = pluginClassInfo.name
                    } catch (e: Exception) {
                        logger.warn("Error reading plugin class info for: {}", pluginClassInfo.name, e)
                    }
                }
            }

        val duration = System.currentTimeMillis() - startMs
        logger.trace("Discovered [{} ms] annotated plugins on classpath: {}", duration, pluginClasses)
        return pluginClasses
    }

    /**
     * Examine the plugin for any required dependencies.
     *
     * @param pluginClass the plugin to examine
     * @return the plugin's dependencies
     */
    private fun examinePlugin(pluginClass: Class<out Plugin>): PluginDependencies {
        val moduleAnnotation = pluginClass.getAnnotation(RequireModules::class.java)
        val requiredModules = if (null != moduleAnnotation && moduleAnnotation.value.isNotEmpty()) {
            instantiateModules(moduleAnnotation)
        } else {
            emptyList()
        }
        return PluginDependencies(requiredModules)
    }

    private fun instantiateModules(moduleAnnotation: RequireModules): List<Module> {
        val modules = mutableListOf<Module>()
        for (moduleClass in moduleAnnotation.value) {
            try {
                modules.add(moduleClass.java.getDeclaredConstructor().newInstance())
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        return modules
    }

    companion object {
        private val logger = LogManager.getLogger(DynamicPluginDiscoveryStrategyImpl::class.java)

        /**
         * The base packages to scan recursively for plugins.
         */
        private val pluginBasePackages = arrayOf(
            ConfigUtil.CURRENT_PACKAGE + ".plugin",
            ConfigUtil.CURRENT_PACKAGE + ".scripting",
            ConfigUtil.CURRENT_PACKAGE + ".store",
        )
    }
}
