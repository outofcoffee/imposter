package com.gatehill.imposter;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.gatehill.imposter.plugin.PluginManager;
import com.gatehill.imposter.service.ResponseService;
import com.gatehill.imposter.service.ResponseServiceImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class ImposterModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ImposterConfig.class).in(Singleton.class);
        bind(PluginManager.class).in(Singleton.class);
        bind(ResponseService.class).to(ResponseServiceImpl.class).in(Singleton.class);
    }
}
