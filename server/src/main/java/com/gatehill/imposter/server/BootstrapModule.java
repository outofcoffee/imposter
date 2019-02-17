package com.gatehill.imposter.server;

import com.gatehill.imposter.ImposterConfig;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class BootstrapModule extends AbstractModule {
    private final String serverFactory;

    BootstrapModule(String serverFactory) {
        this.serverFactory = serverFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
        bind(ImposterConfig.class).in(Singleton.class);

        try {
            final Class<? extends ServerFactory> serverFactoryClass =
                    (Class<? extends ServerFactory>) Class.forName(serverFactory);

            bind(ServerFactory.class).to(serverFactoryClass).in(javax.inject.Singleton.class);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load server factory: " + serverFactory, e);
        }
    }
}
