package com.gatehill.imposter.plugin.test;

import com.gatehill.imposter.plugin.config.ConfiguredPlugin;
import io.vertx.ext.web.Router;

import java.net.HttpURLConnection;
import java.util.List;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class TestPluginImpl extends ConfiguredPlugin<TestPluginConfig> {
    private List<TestPluginConfig> configs;

    @Override
    protected Class<TestPluginConfig> getConfigClass() {
        return TestPluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<TestPluginConfig> configs) {
        this.configs = configs;
    }

    @Override
    public void configureRoutes(Router router) {
        router.get("/example")
                .handler(routingContext -> routingContext.response().setStatusCode(HttpURLConnection.HTTP_OK).end());
    }

    public List<TestPluginConfig> getConfigs() {
        return configs;
    }
}
