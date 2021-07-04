package io.gatehill.imposter.plugin.test;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.ScriptedPlugin;
import io.gatehill.imposter.plugin.config.ConfiguredPlugin;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.gatehill.imposter.service.ResourceService;
import io.gatehill.imposter.service.ResponseService;
import io.vertx.ext.web.Router;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class TestPluginImpl extends ConfiguredPlugin<TestPluginConfig> implements ScriptedPlugin<TestPluginConfig> {
    @Inject
    private ImposterConfig imposterConfig;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResponseService responseService;

    @Override
    protected Class<TestPluginConfig> getConfigClass() {
        return TestPluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<TestPluginConfig> configs) {
        // no-op
    }

    @Override
    public void configureRoutes(Router router) {
        getConfigs().forEach(config -> {
            // root resource
            ofNullable(config.getPath()).ifPresent(path -> configureRoute(config, config, router, path));

            // subresources
            ofNullable(config.getResources()).ifPresent(resources -> resources.forEach(resource ->
                    configureRoute(config, resource, router, resource.getPath())
            ));
        });
    }

    private void configureRoute(TestPluginConfig pluginConfig, ResponseConfigHolder resourceConfig, Router router, String path) {
        router.route(path).handler(resourceService.handleRoute(imposterConfig, pluginConfig, vertx, routingContext -> {
            final Consumer<ResponseBehaviour> defaultBehaviourHandler = responseBehaviour -> {
                responseService.sendResponse(pluginConfig, resourceConfig, routingContext, responseBehaviour);
            };

            scriptHandler(
                    pluginConfig,
                    resourceConfig,
                    routingContext,
                    getInjector(),
                    defaultBehaviourHandler
            );
        }));
    }
}
