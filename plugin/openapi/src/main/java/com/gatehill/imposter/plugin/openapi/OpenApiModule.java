package com.gatehill.imposter.plugin.openapi;

import com.gatehill.imposter.plugin.openapi.service.OpenApiService;
import com.gatehill.imposter.plugin.openapi.service.OpenApiServiceImpl;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(OpenApiService.class).to(OpenApiServiceImpl.class).in(Singleton.class);
    }
}
