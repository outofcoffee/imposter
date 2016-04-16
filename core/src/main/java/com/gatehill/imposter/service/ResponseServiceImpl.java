package com.gatehill.imposter.service;

import com.gatehill.imposter.ImposterConfig;
import com.gatehill.imposter.model.InvocationContext;
import com.gatehill.imposter.model.ResponseBehaviour;
import com.gatehill.imposter.plugin.config.BaseConfig;
import com.google.common.io.CharStreams;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseServiceImpl implements ResponseService {
    private static final Logger LOGGER = LogManager.getLogger(ResponseServiceImpl.class);

    @Inject
    private ImposterConfig imposterConfig;

    @Override
    public ResponseBehaviour getResponseBehaviour(RoutingContext routingContext, BaseConfig config) {
        final int statusCode = ofNullable(config.getResponseConfig().getStatusCode()).orElse(HttpURLConnection.HTTP_OK);
        if (null == config.getResponseConfig().getScriptFile()) {
            // default handling
            LOGGER.debug("Using default response behaviour for request: {}", routingContext.request().absoluteURI());
            return ResponseBehaviour.buildStatic(statusCode, imposterConfig, config.getResponseConfig().getStaticFile());
        }

        try {
            LOGGER.debug("Using scripted response behaviour for request: {}", routingContext.request().absoluteURI());

            final Binding binding = new Binding();
            binding.setVariable("config", config);
            binding.setVariable("context", InvocationContext.build(routingContext));

            final GroovyShell groovyShell = new GroovyShell(binding);
            groovyShell.evaluate(Paths.get(imposterConfig.getConfigDir(), config.getResponseConfig().getScriptFile()).toFile());

            final InvocationContext invocationContext = (InvocationContext) binding.getVariable("context");
            LOGGER.debug("InvocationContext for request: {}", invocationContext);

            if (invocationContext.isHandled()) {
                return ResponseBehaviour.buildHandled(invocationContext.getStatusCode());

            } else {
                // default handling
                return ResponseBehaviour.buildStatic(statusCode, imposterConfig, config.getResponseConfig().getStaticFile());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream loadResponseAsStream(ImposterConfig imposterConfig, RoutingContext routingContext, BaseConfig mockConfig) throws IOException {
        final ResponseBehaviour behaviour = getResponseBehaviour(routingContext, mockConfig);
        if (null != behaviour.getResponseFile()) {
            return Files.newInputStream(behaviour.getResponseFile());
        } else {
            throw new IllegalStateException("No response file set on ResponseBehaviour");
        }
    }

    @Override
    public JsonArray loadResponseAsJsonArray(ImposterConfig imposterConfig, RoutingContext routingContext, BaseConfig config) {
        try (InputStream is = loadResponseAsStream(imposterConfig, routingContext, config)) {
            return new JsonArray(CharStreams.toString(new InputStreamReader(is)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
