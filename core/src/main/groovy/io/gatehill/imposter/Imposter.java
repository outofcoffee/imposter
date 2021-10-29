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

package io.gatehill.imposter;

import com.google.inject.Injector;
import com.google.inject.Module;
import io.gatehill.imposter.plugin.PluginDependencies;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.util.ConfigUtil;
import io.gatehill.imposter.util.InjectorUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static io.gatehill.imposter.util.HttpUtil.BIND_ALL_HOSTS;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish
 */
public class Imposter {
    private static final Logger LOGGER = LogManager.getLogger(Imposter.class);

    private final ImposterConfig imposterConfig;
    private final List<Module> bootstrapModules;
    private final PluginManager pluginManager;

    public Imposter(ImposterConfig imposterConfig, Module... bootstrapModules) {
        this(imposterConfig, newArrayList(bootstrapModules));
    }

    public Imposter(ImposterConfig imposterConfig, List<Module> bootstrapModules) {
        this(imposterConfig, bootstrapModules, new PluginManager());
    }

    public Imposter(ImposterConfig imposterConfig, List<Module> bootstrapModules, PluginManager pluginManager) {
        this.imposterConfig = imposterConfig;
        this.bootstrapModules = bootstrapModules;
        this.pluginManager = pluginManager;
    }

    public void start() {
        LOGGER.info("Starting mock engine");

        // load config
        processConfiguration();
        final Map<String, List<File>> pluginConfigs = ConfigUtil.loadPluginConfigs(pluginManager, imposterConfig.getConfigDirs());

        final List<String> plugins = ofNullable(imposterConfig.getPlugins())
                .map(it -> (List<String>) newArrayList(it))
                .orElse(emptyList());

        final List<PluginDependencies> dependencies = pluginManager.preparePluginsFromConfig(imposterConfig, plugins, pluginConfigs)
                .stream()
                .filter(deps -> nonNull(deps.getRequiredModules()))
                .collect(Collectors.toList());

        final List<Module> allModules = bootstrapModules;
        allModules.add(new ImposterModule(imposterConfig, pluginManager));
        dependencies.forEach(deps -> allModules.addAll(deps.getRequiredModules()));

        // inject dependencies
        final Injector injector = InjectorUtil.create(allModules.toArray(new Module[0]));
        injector.injectMembers(this);
        pluginManager.registerPlugins(injector);
        pluginManager.configurePlugins(pluginConfigs);
    }

    private void processConfiguration() {
        imposterConfig.setServerUrl(buildServerUrl().toString());

        final String[] configDirs = imposterConfig.getConfigDirs();

        // resolve relative config paths
        for (int i = 0; i < configDirs.length; i++) {
            if (configDirs[i].startsWith("./")) {
                configDirs[i] = Paths.get(System.getProperty("user.dir"), configDirs[i].substring(2)).toString();
            }
        }
    }

    private URI buildServerUrl() {
        // might be set explicitly
        final Optional<String> explicitUrl = ofNullable(imposterConfig.getServerUrl());
        if (explicitUrl.isPresent()) {
            return URI.create(explicitUrl.get());
        }

        // build based on configuration
        final String scheme = (imposterConfig.isTlsEnabled() ? "https" : "http") + "://";
        final String host = (BIND_ALL_HOSTS.equals(imposterConfig.getHost()) ? "localhost" : imposterConfig.getHost());

        final String port;
        if ((imposterConfig.isTlsEnabled() && 443 == imposterConfig.getListenPort())
                || (!imposterConfig.isTlsEnabled() && 80 == imposterConfig.getListenPort())) {
            port = "";
        } else {
            port = ":" + imposterConfig.getListenPort();
        }

        return URI.create(scheme + host + port);
    }
}
