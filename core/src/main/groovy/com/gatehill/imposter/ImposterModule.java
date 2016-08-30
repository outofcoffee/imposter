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
    @Override
    protected void configure() {
        bind(PluginManager.class).in(Singleton.class);
        bind(ResponseService.class).to(ResponseServiceImpl.class).in(Singleton.class);
        bind(ScriptService.class).annotatedWith(GroovyImpl.class).to(GroovyScriptServiceImpl.class).in(Singleton.class);
        bind(ScriptService.class).annotatedWith(JavascriptImpl.class).to(NashhornScriptServiceImpl.class).in(Singleton.class);
    }
}
