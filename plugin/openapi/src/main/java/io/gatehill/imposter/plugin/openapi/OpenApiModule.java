package io.gatehill.imposter.plugin.openapi;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.gatehill.imposter.plugin.openapi.service.OpenApiService;
import io.gatehill.imposter.plugin.openapi.service.OpenApiServiceImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(OpenApiService.class).to(OpenApiServiceImpl.class).in(Singleton.class);
    }
}
