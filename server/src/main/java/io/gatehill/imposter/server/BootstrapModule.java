package io.gatehill.imposter.server;

import com.google.inject.AbstractModule;
import io.vertx.core.Vertx;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class BootstrapModule extends AbstractModule {
    private final Vertx vertx;
    private final String serverFactory;

    public BootstrapModule(Vertx vertx, String serverFactory) {
        this.vertx = vertx;
        this.serverFactory = serverFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
        bind(Vertx.class).toInstance(vertx);

        try {
            final Class<? extends ServerFactory> serverFactoryClass =
                    (Class<? extends ServerFactory>) Class.forName(serverFactory);

            bind(ServerFactory.class).to(serverFactoryClass).in(javax.inject.Singleton.class);

        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not load server factory: " + serverFactory, e);
        }
    }
}
