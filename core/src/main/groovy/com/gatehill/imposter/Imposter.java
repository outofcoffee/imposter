package com.gatehill.imposter;

import com.gatehill.imposter.plugin.PluginDependencies;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.util.ConfigUtil;
import com.gatehill.imposter.util.InjectorUtil;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.gatehill.imposter.util.HttpUtil.BIND_ALL_HOSTS;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class Imposter {
    private static final Logger LOGGER = LogManager.getLogger(Imposter.class);

    private final ImposterConfig imposterConfig;
    private final Module[] bootstrapModules;
    private final PluginManager pluginManager;

    public Imposter(ImposterConfig imposterConfig, Module... bootstrapModules) {
        this(imposterConfig, bootstrapModules, new PluginManager());
    }

    public Imposter(ImposterConfig imposterConfig, Module[] bootstrapModules, PluginManager pluginManager) {
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

        final List<Module> allModules = newArrayList(bootstrapModules);
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
