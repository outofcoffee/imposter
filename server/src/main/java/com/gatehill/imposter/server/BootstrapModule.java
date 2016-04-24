package com.gatehill.imposter.server;

import com.gatehill.imposter.ImposterConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class BootstrapModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(ImposterConfig.class).in(Singleton.class);
    }
}
