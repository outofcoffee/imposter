package com.gatehill.imposter;

import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.service.*;
import com.gatehill.imposter.util.annotation.GroovyImpl;
import com.gatehill.imposter.util.annotation.JavascriptImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class ImposterModule extends AbstractModule {
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
        bind(ResponseService.class).to(ResponseServiceImpl.class).in(Singleton.class);
        bind(ScriptService.class).annotatedWith(GroovyImpl.class).to(GroovyScriptServiceImpl.class).in(Singleton.class);
        bind(ScriptService.class).annotatedWith(JavascriptImpl.class).to(NashhornScriptServiceImpl.class).in(Singleton.class);
    }
}
