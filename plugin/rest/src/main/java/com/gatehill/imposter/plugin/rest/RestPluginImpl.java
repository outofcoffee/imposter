package com.gatehill.imposter.plugin.rest;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.config.ConfiguredPlugin;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

import javax.inject.Inject;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginImpl extends ConfiguredPlugin<RestPluginConfig> {
    @Inject
    private ImposterConfig imposterConfig;

    private List<RestPluginConfig> configs;

    @Override
    protected Class<RestPluginConfig> getConfigClass() {
        return RestPluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<RestPluginConfig> configs) {
        this.configs = configs;
    }

    @Override
    public void configureRoutes(Router router) {
        configs.forEach(config -> {
            router.get(config.getBaseUrl())
                    .handler(routingContext -> {
                        final HttpServerResponse response = routingContext.response();

                        ofNullable(config.getContentType())
                                .ifPresent(contentType -> response.putHeader("Content-Type", contentType));

                        final Path responseFile = Paths.get(imposterConfig.getConfigDir(), config.getResponseFile());
                        response.setStatusCode(HttpURLConnection.HTTP_OK)
                                .sendFile(responseFile.toString());
                    });
        });
    }
}
