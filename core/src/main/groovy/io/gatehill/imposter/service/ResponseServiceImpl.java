package io.gatehill.imposter.service;

import com.google.common.base.Strings;
import com.google.common.io.CharStreams;
import io.gatehill.imposter.config.ResolvedResourceConfig;
import io.gatehill.imposter.exception.ResponseException;
import io.gatehill.imposter.http.ResponseBehaviourFactory;
import io.gatehill.imposter.http.StatusCodeFactory;
import io.gatehill.imposter.plugin.config.ContentTypedConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.ResourcesHolder;
import io.gatehill.imposter.plugin.config.resource.PathParamsResourceConfig;
import io.gatehill.imposter.plugin.config.resource.QueryParamsResourceConfig;
import io.gatehill.imposter.plugin.config.resource.ResourceMethod;
import io.gatehill.imposter.plugin.config.resource.ResponseConfig;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;
import io.gatehill.imposter.script.ExecutionContext;
import io.gatehill.imposter.script.ReadWriteResponseBehaviour;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.gatehill.imposter.script.ResponseBehaviourType;
import io.gatehill.imposter.script.RuntimeContext;
import io.gatehill.imposter.script.ScriptUtil;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
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
            StatusCodeFactory statusCodeFactory,
            ResponseBehaviourFactory responseBehaviourFactory
    ) {
        final ResponseConfig responseConfig = resourceConfig.getResponseConfig();

        checkNotNull(responseConfig, "Response configuration must not be null");
        final int statusCode = statusCodeFactory.calculateStatus(resourceConfig);

        if (isNull(responseConfig.getScriptFile())) {
            LOGGER.debug("Using default HTTP {} response behaviour for request: {} {}",
                    statusCode, routingContext.request().method(), routingContext.request().absoluteURI());

            return responseBehaviourFactory.build(statusCode, responseConfig);
        }

        return determineResponseFromScript(routingContext, pluginConfig, resourceConfig, additionalContext, additionalBindings, statusCode);
    }

    @Override
    public List<ResolvedResourceConfig> resolveResourceConfigs(ResponseConfigHolder config) {
        if (config instanceof ResourcesHolder) {
            @SuppressWarnings("unchecked") final ResourcesHolder<RestResourceConfig> resources = (ResourcesHolder<RestResourceConfig>) config;

            if (nonNull(resources.getResources())) {
                return resources.getResources().stream()
                        .map(res -> new ResolvedResourceConfig(res, findPathParams(res), findQueryParams(res)))
                        .collect(Collectors.toList());
            }
        }
        return emptyList();
    }

    private Map<String, String> findPathParams(ResponseConfigHolder responseConfigHolder) {
        if (responseConfigHolder instanceof PathParamsResourceConfig) {
            final Map<String, String> params = ((PathParamsResourceConfig) responseConfigHolder).getPathParams();
            return ofNullable(params).orElse(emptyMap());
        }
        return emptyMap();
    }

    private Map<String, String> findQueryParams(ResponseConfigHolder responseConfigHolder) {
        if (responseConfigHolder instanceof QueryParamsResourceConfig) {
            final Map<String, String> params = ((QueryParamsResourceConfig) responseConfigHolder).getQueryParams();
            return ofNullable(params).orElse(emptyMap());
        }
        return emptyMap();
    }

    /**
     * Search for a resource configuration matching the request.
     *
     * @param resources    the resources from the response configuration
     * @param method       the HTTP method of the current request
     * @param pathTemplate request path template
     * @param path         the path of the current request
     * @param pathParams   the path parameters of the current request
     * @param queryParams  the query parameters of the current request
     * @return a matching resource configuration or else empty
     */
    @Override
    public Optional<ResponseConfigHolder> matchResourceConfig(
            List<ResolvedResourceConfig> resources,
            HttpMethod method,
            String pathTemplate,
            String path,
            Map<String, String> pathParams,
            Map<String, String> queryParams
    ) {
        final ResourceMethod resourceMethod = ResourceMethodConverter.convertMethodFromVertx(method);

        List<ResolvedResourceConfig> resourceConfigs = resources.stream()
                .filter(res -> isRequestMatch(res, resourceMethod, pathTemplate, path, pathParams, queryParams))
                .collect(Collectors.toList());

        // find the most specific, by filter those that match for those that specify parameters
        resourceConfigs = filterByParams(resourceConfigs, ResolvedResourceConfig::getPathParams);
        resourceConfigs = filterByParams(resourceConfigs, ResolvedResourceConfig::getQueryParams);

        if (resourceConfigs.isEmpty()) {
            return empty();
        }

        if (resourceConfigs.size() == 1) {
            LOGGER.debug("Matched response config for {} {}", resourceMethod, path);
        } else {
            LOGGER.warn("More than one response config found for {} {} - this is probably a configuration error. Choosing first response configuration.", resourceMethod, path);
        }
        return of(resourceConfigs.get(0).getConfig());
    }

    private List<ResolvedResourceConfig> filterByParams(
            List<ResolvedResourceConfig> resourceConfigs,
            Function<ResolvedResourceConfig, Map<String, String>> paramsSupplier
    ) {
        final List<ResolvedResourceConfig> configsWithParams = resourceConfigs.stream()
                .filter(res -> !paramsSupplier.apply(res).isEmpty())
                .collect(Collectors.toList());

        if (configsWithParams.isEmpty()) {
            // no resource configs specified params - don't filter
            return resourceConfigs;
        } else {
            return configsWithParams;
        }
    }

    /**
     * Determine if the resource configuration matches the current request.
     *
     * @param resource       the resource configuration
     * @param resourceMethod the HTTP method of the current request
     * @param pathTemplate   request path template
     * @param path           the path of the current request
     * @param pathParams     the path parameters of the current request
     * @param queryParams    the query parameters of the current request
     * @return {@code true} if the the resource matches the request, otherwise {@code false}
     */
    private boolean isRequestMatch(
            ResolvedResourceConfig resource,
            ResourceMethod resourceMethod,
            String pathTemplate,
            String path,
            Map<String, String> pathParams,
            Map<String, String> queryParams
    ) {
        final RestResourceConfig resourceConfig = resource.getConfig();
        final boolean pathMatch = path.equals(resourceConfig.getPath()) || pathTemplate.equals(resourceConfig.getPath());

        return pathMatch &&
                resourceMethod.equals(resourceConfig.getMethod()) &&
                matchParams(pathParams, resource.getPathParams()) &&
                matchParams(queryParams, resource.getQueryParams());
    }

    /**
     * If the resource contains parameter configuration, check they are all present.
     * If the configuration contains no parameters, then this evaluates to true.
     * Additional parameters not in the configuration are ignored.
     *
     * @param resourceParams the configured parameters to match
     * @param requestParams  the parameters from the request (e.g. query or path)
     * @return {@code true} if the configured parameters match the request, otherwise {@code false}
     */
    private boolean matchParams(Map<String, String> requestParams, Map<String, String> resourceParams) {
        // none configured - implies any match
        if (resourceParams.isEmpty()) {
            return true;
        }
        return resourceParams.entrySet().stream().allMatch(paramConfig ->
                HttpUtil.safeEquals(requestParams.get(paramConfig.getKey()), paramConfig.getValue())
        );
    }

    private ReadWriteResponseBehaviour determineResponseFromScript(
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
            final ReadWriteResponseBehaviour responseBehaviour =
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
