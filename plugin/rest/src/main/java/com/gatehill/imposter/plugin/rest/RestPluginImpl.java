package com.gatehill.imposter.plugin.rest;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.plugin.ScriptedPlugin;
import com.gatehill.imposter.plugin.config.ConfiguredPlugin;
import com.gatehill.imposter.util.HttpUtil;
import com.google.common.base.Strings;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.nio.file.Paths;
import java.util.List;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RestPluginImpl<C extends RestPluginConfig> extends ConfiguredPlugin<C> implements ScriptedPlugin<C> {
    private static final Logger LOGGER = LogManager.getLogger(RestPluginImpl.class);

    @Inject
    private ImposterConfig imposterConfig;

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
        configs.forEach(config -> router.get(config.getBasePath()).handler(routingContext -> {
            // script should fire first
            scriptHandler(config, routingContext, responseBehaviour -> {
                LOGGER.info("Handling request for: {}", routingContext.request().absoluteURI());

                final HttpServerResponse response = routingContext.response();

                ofNullable(config.getContentType())
                        .ifPresent(contentType -> response.putHeader(HttpUtil.CONTENT_TYPE, contentType));

                try {
                    response.setStatusCode(responseBehaviour.getStatusCode());

                    if (Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
                        LOGGER.debug("Response file blank - returning empty response");
                        response.end();

                    } else {
                        response.sendFile(Paths.get(imposterConfig.getConfigDir(), responseBehaviour.getResponseFile()).toString());
                    }

                } catch (Exception e) {
                    routingContext.fail(e);
                }
            });

        }));
    }
}
