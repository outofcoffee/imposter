package io.gatehill.imposter.plugin.openapi;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.gatehill.imposter.plugin.openapi.service.ExampleService;
import io.gatehill.imposter.plugin.openapi.service.ExampleServiceImpl;
import io.gatehill.imposter.plugin.openapi.service.ModelService;
import io.gatehill.imposter.plugin.openapi.service.ModelServiceImpl;
import io.gatehill.imposter.plugin.openapi.service.SpecificationService;
import io.gatehill.imposter.plugin.openapi.service.SpecificationServiceImpl;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OpenApiModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(SpecificationService.class).to(SpecificationServiceImpl.class).in(Singleton.class);
        bind(ExampleService.class).to(ExampleServiceImpl.class).in(Singleton.class);
        bind(ModelService.class).to(ModelServiceImpl.class).in(Singleton.class);
    }
}
