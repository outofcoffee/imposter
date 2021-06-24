package io.gatehill.imposter;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.gatehill.imposter.plugin.PluginManager;
import io.gatehill.imposter.service.ResourceService;
import io.gatehill.imposter.service.ResourceServiceImpl;
import io.gatehill.imposter.service.ResponseService;
import io.gatehill.imposter.service.ResponseServiceImpl;
import io.gatehill.imposter.service.SecurityService;
import io.gatehill.imposter.service.SecurityServiceImpl;
import io.gatehill.imposter.store.StoreModule;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class ImposterModule extends AbstractModule {
    private static final Logger LOGGER = LogManager.getLogger(ImposterModule.class);

    private final ImposterConfig imposterConfig;
    private final PluginManager pluginManager;

    public ImposterModule(ImposterConfig imposterConfig, PluginManager pluginManager) {
        this.imposterConfig = imposterConfig;
        this.pluginManager = pluginManager;
    }

    @Override
    protected void configure() {
        bind(ImposterConfig.class).toInstance(imposterConfig);
        bind(PluginManager.class).toInstance(pluginManager);
        bind(ResourceService.class).to(ResourceServiceImpl.class).in(Singleton.class);
        bind(ResponseService.class).to(ResponseServiceImpl.class).in(Singleton.class);
        bind(SecurityService.class).to(SecurityServiceImpl.class).in(Singleton.class);

        configureExperimentalFeatures();
    }

    private void configureExperimentalFeatures() {
        final List<String> experimentalFeatures = listExperimentalFeatures();
        LOGGER.trace("Experimental features enabled: {}", experimentalFeatures);

        if (experimentalFeatures.contains("store")) {
            install(new StoreModule());
        }
    }

    private List<String> listExperimentalFeatures() {
        return Arrays.asList(ofNullable(System.getenv("IMPOSTER_EXPERIMENTAL"))
                .orElse(ofNullable(System.getProperty("imposter.experimental")).orElse(""))
                .split(","));
    }
}
