package com.gatehill.imposter.plugin.rest;

import com.gatehill.imposter.model.ResponseBehaviour;
import com.gatehill.imposter.plugin.config.ConfiguredPlugin;
import com.gatehill.imposter.service.ResponseService;
import com.gatehill.imposter.util.HttpUtil;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginImpl<C extends RestPluginConfig> extends ConfiguredPlugin<C> {
    private static final Logger LOGGER = LogManager.getLogger(RestPluginImpl.class);

    @Inject
    private ResponseService responseService;

    private List<C> configs;

    @SuppressWarnings("unchecked")
    @Override
    protected Class<C> getConfigClass() {
        return (Class<C>) RestPluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<C> configs) {
        this.configs = configs;
    }

    @Override
    public void configureRoutes(Router router) {
        configs.forEach(config -> {
            router.get(config.getBasePath())
                    .handler(routingContext -> {
                        LOGGER.info("Handling request for: {}", routingContext.request().absoluteURI());

                        final HttpServerResponse response = routingContext.response();

                        ofNullable(config.getContentType())
                                .ifPresent(contentType -> response.putHeader(HttpUtil.CONTENT_TYPE, contentType));

                        try {
                            final ResponseBehaviour responseBehaviour = responseService.getResponseBehaviour(routingContext, config);
                            response.setStatusCode(responseBehaviour.getStatusCode());

                            if (responseBehaviour.isHandled()) {
                                response.end();
                            } else {
                                response.sendFile(responseBehaviour.getResponseFile().toString());
                            }

                        } catch (Exception e) {
                            routingContext.fail(e);
                        }
                    });
        });
    }

    protected List<C> getConfigs() {
        return configs;
    }
}
