package io.gatehill.imposter.service;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import io.gatehill.imposter.exception.ResponseException;
import io.gatehill.imposter.http.StatusCodeCalculator;
import io.gatehill.imposter.plugin.config.ContentTypedConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.ResourcesHolder;
import io.gatehill.imposter.plugin.config.resource.ResourceMethod;
import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;
import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.gatehill.imposter.script.ResponseBehaviourType;
import io.gatehill.imposter.script.RuntimeContext;
import io.gatehill.imposter.script.ScriptUtil;
import io.gatehill.imposter.script.ScriptedResponseBehavior;
import io.gatehill.imposter.script.impl.ScriptedResponseBehaviorImpl;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.ResourceMethodConverter;
import io.gatehill.imposter.util.annotation.GroovyImpl;
import io.gatehill.imposter.util.annotation.JavascriptImpl;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ResponseServiceImpl implements ResponseService {
    private static final Logger LOGGER = LogManager.getLogger(ResponseServiceImpl.class);

    @Inject
    @GroovyImpl
    private ScriptService groovyScriptService;

    @Inject
    @JavascriptImpl
    private ScriptService javascriptScriptService;

    @Override
    public ResponseBehaviour buildResponseBehaviour(
            RoutingContext routingContext,
            PluginConfig pluginConfig,
            ResponseConfigHolder resourceConfig,
            Map<String, Object> additionalContext,
            Map<String, Object> additionalBindings,
            StatusCodeCalculator statusCodeCalculator
    ) {
        final ResponseConfig responseConfig = resourceConfig.getResponseConfig();

        checkNotNull(responseConfig, "Response configuration must not be null");
        final int statusCode = statusCodeCalculator.calculateStatus(resourceConfig);

        if (isNull(responseConfig.getScriptFile())) {
            LOGGER.debug("Using default HTTP {} response behaviour for request: {} {}",
                    statusCode, routingContext.request().method(), routingContext.request().absoluteURI());

            final ScriptedResponseBehaviorImpl responseBehaviour = new ScriptedResponseBehaviorImpl();
            responseBehaviour
                    .withStatusCode(statusCode)
                    .withFile(responseConfig.getStaticFile())
                    .withData(responseConfig.getStaticData())
                    .usingDefaultBehaviour();

            ofNullable(responseConfig.getHeaders()).orElse(emptyMap())
                    .forEach(responseBehaviour::withHeader);

            return responseBehaviour;
        }

        return determineResponseFromScript(routingContext, pluginConfig, resourceConfig, additionalContext, additionalBindings, statusCode);
    }

    /**
     * Search for a resource configuration matching the request.
     *
     * @param config the response configuration
     * @param path   the request path
     * @param method the HTTP method
     * @return a matching resource configuration or else empty
     */
    @Override
    public Optional<ResponseConfigHolder> findResourceConfig(PluginConfig config, String path, HttpMethod method) {
        if (config instanceof ResourcesHolder) {
            @SuppressWarnings("unchecked") final ResourcesHolder<RestResourceConfig> resources = (ResourcesHolder<RestResourceConfig>) config;
            final ResourceMethod resourceMethod = ResourceMethodConverter.convertMethodFromVertx(method);

            if (nonNull(resources.getResources())) {
                final Optional<ResponseConfigHolder> resourceConfig = resources.getResources().stream()
                        .filter(res -> path.equals(res.getPath()))
                        .filter(res -> resourceMethod.equals(res.getMethod()))
                        .map(res -> (ResponseConfigHolder) res)
                        .findAny();

                if (resourceConfig.isPresent()) {
                    LOGGER.debug("Matched default response config for {} {}", resourceMethod, path);
                }

                return resourceConfig;
            }
        }
        return empty();
    }

    private ScriptedResponseBehavior determineResponseFromScript(
            RoutingContext routingContext,
            PluginConfig pluginConfig,
            ResponseConfigHolder resourceConfig,
            Map<String, Object> additionalContext,
            Map<String, Object> additionalBindings,
            int statusCode
    ) {
        final ResponseConfig responseConfig = resourceConfig.getResponseConfig();

        try {
            LOGGER.debug("Executing script '{}' for request: {} {}",
                    responseConfig.getScriptFile(), routingContext.request().method(), routingContext.request().absoluteURI());

            final ExecutionContext executionContext = ScriptUtil.buildContext(routingContext, additionalContext);
            LOGGER.trace("Context for request: {}", () -> executionContext);

            final RuntimeContext runtimeContext = new RuntimeContext(
                    LogManager.getLogger(determineScriptName(responseConfig.getScriptFile())),
                    pluginConfig,
                    additionalBindings,
                    executionContext
            );

            // execute the script and read response behaviour
            final ScriptedResponseBehavior responseBehaviour =
                    fetchScriptService(responseConfig.getScriptFile()).executeScript(pluginConfig, resourceConfig, runtimeContext);

            // use defaults if not set
            if (ResponseBehaviourType.DEFAULT_BEHAVIOUR.equals(responseBehaviour.getBehaviourType())) {
                if (Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
                    responseBehaviour.withFile(responseConfig.getStaticFile());
                }
                if (0 == responseBehaviour.getStatusCode()) {
                    responseBehaviour.withStatusCode(statusCode);
                }
            }

            return responseBehaviour;

        } catch (Exception e) {
            throw new RuntimeException(String.format("Error executing script: %s", responseConfig.getScriptFile()), e);
        }
    }

    @Override
    public boolean sendEmptyResponse(RoutingContext routingContext, ResponseBehaviour responseBehaviour) {
        try {
            LOGGER.info("Response file and data are blank - returning empty response");
            routingContext.response().end();
            return true;
        } catch (Exception e) {
            LOGGER.warn("Error sending empty response", e);
            return false;
        }
    }

    @Override
    public void sendResponse(PluginConfig pluginConfig,
                             ContentTypedConfig resourceConfig,
                             RoutingContext routingContext,
                             ResponseBehaviour responseBehaviour) {
        sendResponse(pluginConfig, resourceConfig, routingContext, responseBehaviour, this::sendEmptyResponse);
    }

    @Override
    public void sendResponse(PluginConfig pluginConfig,
                             ContentTypedConfig resourceConfig,
                             RoutingContext routingContext,
                             ResponseBehaviour responseBehaviour,
                             ResponseSender... fallbackSenders) {

        LOGGER.trace("Sending mock response for URI {} with status code {}",
                routingContext.request().absoluteURI(),
                responseBehaviour.getStatusCode());

        try {
            final HttpServerResponse response = routingContext.response();
            response.setStatusCode(responseBehaviour.getStatusCode());

            responseBehaviour.getResponseHeaders().forEach(response::putHeader);

            if (!Strings.isNullOrEmpty(responseBehaviour.getResponseFile())) {
                serveResponseFile(pluginConfig, routingContext, responseBehaviour);
            } else if (!Strings.isNullOrEmpty(responseBehaviour.getResponseData())) {
                serveResponseData(resourceConfig, routingContext, responseBehaviour);
            } else {
                fallback(routingContext, responseBehaviour, fallbackSenders);
            }

        } catch (Exception e) {
            routingContext.fail(new ResponseException(String.format(
                    "Error sending mock response for URI %s with status code %s",
                    routingContext.request().absoluteURI(), responseBehaviour.getStatusCode()), e));
        }
    }

    /**
     * Reply with a static response file. Note that the content type is determined
     * by the file being sent.
     *
     * @param pluginConfig      the plugin configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     */
    private void serveResponseFile(PluginConfig pluginConfig,
                                   RoutingContext routingContext,
                                   ResponseBehaviour responseBehaviour) {

        LOGGER.info("Serving response file {} for URI {} with status code {}",
                responseBehaviour.getResponseFile(),
                routingContext.request().absoluteURI(),
                routingContext.response().getStatusCode());

        routingContext.response().sendFile(
                Paths.get(pluginConfig.getParentDir().getAbsolutePath(), responseBehaviour.getResponseFile()).toString());
    }

    /**
     * Reply with the contents of a String. Content type should be provided, but if not
     * JSON is assumed.
     *
     * @param resourceConfig    the resource configuration
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     */
    private void serveResponseData(ContentTypedConfig resourceConfig,
                                   RoutingContext routingContext,
                                   ResponseBehaviour responseBehaviour) {

        LOGGER.info("Serving response data ({} bytes) for URI {} with status code {}",
                responseBehaviour.getResponseData().length(),
                routingContext.request().absoluteURI(),
                routingContext.response().getStatusCode());

        final HttpServerResponse response = routingContext.response();

        // explicit content type
        if (!Strings.isNullOrEmpty(resourceConfig.getContentType())) {
            response.putHeader(HttpUtil.CONTENT_TYPE, resourceConfig.getContentType());
        }

        if (!response.headers().contains(HttpUtil.CONTENT_TYPE)) {
            // consider something like Tika to probe content type
            LOGGER.debug("Guessing JSON content type");
            response.putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON);
        }

        response.end(responseBehaviour.getResponseData());
    }

    private ScriptService fetchScriptService(String scriptFile) {
        final String scriptExtension;
        final int dotIndex = scriptFile.lastIndexOf('.');
        if (dotIndex >= 1 && dotIndex < scriptFile.length() - 1) {
            scriptExtension = scriptFile.substring(dotIndex + 1);
        } else {
            scriptExtension = "";
        }

        switch (scriptExtension.toLowerCase()) {
            case "groovy":
                return groovyScriptService;
            case "js":
                return javascriptScriptService;
            default:
                throw new RuntimeException("Unable to determine script engine from script file name: " + scriptFile);
        }
    }

    private void fallback(RoutingContext routingContext,
                          ResponseBehaviour responseBehaviour,
                          ResponseSender[] missingResponseSenders) {

        if (nonNull(missingResponseSenders)) {
            for (ResponseSender sender : missingResponseSenders) {
                try {
                    if (sender.send(routingContext, responseBehaviour)) {
                        return;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Error invoking response sender", e);
                }
            }
        }
        throw new ResponseException("All attempts to send a response failed");
    }

    private String determineScriptName(String scriptFile) {
        final int dotIndex = scriptFile.lastIndexOf('.');
        if (dotIndex >= 1 && dotIndex < scriptFile.length() - 1) {
            return scriptFile.substring(0, dotIndex);
        } else {
            return scriptFile;
        }
    }

    private InputStream loadResponseAsStream(PluginConfig config, String responseFile) throws IOException {
        if (null != responseFile) {
            return Files.newInputStream(Paths.get(config.getParentDir().getAbsolutePath(), responseFile));
        } else {
            throw new IllegalStateException("No response file set on ResponseBehaviour");
        }
    }

    @Override
    public JsonArray loadResponseAsJsonArray(PluginConfig config, ResponseBehaviour behaviour) {
        return loadResponseAsJsonArray(config, behaviour.getResponseFile());
    }

    @Override
    public JsonArray loadResponseAsJsonArray(PluginConfig config, String responseFile) {
        if (Strings.isNullOrEmpty(responseFile)) {
            LOGGER.debug("Response file blank - returning empty array");
            return new JsonArray();
        }

        try (InputStream is = loadResponseAsStream(config, responseFile)) {
            return new JsonArray(CharStreams.toString(new InputStreamReader(is)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
