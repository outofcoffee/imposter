package io.gatehill.imposter.server;

import com.google.inject.AbstractModule;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.lifecycle.ImposterLifecycleHooks;
import io.vertx.core.Vertx;

import javax.inject.Singleton;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class BootstrapModule extends AbstractModule {
    private final Vertx vertx;
    private final ImposterConfig imposterConfig;
    private final String serverFactory;

    public BootstrapModule(Vertx vertx, ImposterConfig imposterConfig, String serverFactory) {
        this.vertx = vertx;
        this.imposterConfig = imposterConfig;
        this.serverFactory = serverFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
        bind(Vertx.class).toInstance(vertx);
        bind(ImposterConfig.class).toInstance(imposterConfig);

        try {
            final Class<? extends ServerFactory> serverFactoryClass =
                    (Class<? extends ServerFactory>) Class.forName(serverFactory);

            bind(ServerFactory.class).to(serverFactoryClass).in(Singleton.class);
            bind(ImposterLifecycleHooks.class).in(Singleton.class);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load server factory: " + serverFactory, e);
        }
    }
}
