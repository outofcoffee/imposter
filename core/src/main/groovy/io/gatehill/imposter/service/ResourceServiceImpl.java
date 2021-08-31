package io.gatehill.imposter.service;

import com.google.common.base.Strings;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.config.ResolvedResourceConfig;
import io.gatehill.imposter.lifecycle.ImposterLifecycleHooks;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.ResourcesHolder;
import io.gatehill.imposter.plugin.config.resource.PathParamsResourceConfig;
import io.gatehill.imposter.plugin.config.resource.QueryParamsResourceConfig;
import io.gatehill.imposter.plugin.config.resource.RequestHeadersResourceConfig;
import io.gatehill.imposter.plugin.config.resource.ResourceConfig;
import io.gatehill.imposter.plugin.config.resource.ResourceMethod;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.plugin.config.resource.RestResourceConfig;
import io.gatehill.imposter.plugin.config.resource.reqbody.RequestBodyConfig;
import io.gatehill.imposter.util.CollectionUtil;
import io.gatehill.imposter.util.ResourceUtil;
import io.gatehill.imposter.util.StringUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static io.gatehill.imposter.util.HttpUtil.convertMultiMapToHashMap;
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
public class ResourceServiceImpl implements ResourceService {
    private static final Logger LOGGER = LogManager.getLogger(ResourceServiceImpl.class);

    @Inject
    private SecurityService securityService;

    @Inject
    private ImposterLifecycleHooks lifecycleHooks;

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ResolvedResourceConfig> resolveResourceConfigs(PluginConfig pluginConfig) {
        if (pluginConfig instanceof ResourcesHolder) {
            @SuppressWarnings("unchecked") final ResourcesHolder<RestResourceConfig> resources = (ResourcesHolder<RestResourceConfig>) pluginConfig;

            if (nonNull(resources.getResources())) {
                return resources.getResources().stream()
                        .map(res -> new ResolvedResourceConfig(res, findPathParams(res), findQueryParams(res), findRequestHeaders(res)))
                        .collect(Collectors.toList());
            }
        }
        return emptyList();
    }

    private Map<String, String> findPathParams(ResourceConfig resourceConfig) {
        if (resourceConfig instanceof PathParamsResourceConfig) {
            final Map<String, String> params = ((PathParamsResourceConfig) resourceConfig).getPathParams();
            return ofNullable(params).orElse(emptyMap());
        }
        return emptyMap();
    }

    private Map<String, String> findQueryParams(ResourceConfig resourceConfig) {
        if (resourceConfig instanceof QueryParamsResourceConfig) {
            final Map<String, String> params = ((QueryParamsResourceConfig) resourceConfig).getQueryParams();
            return ofNullable(params).orElse(emptyMap());
        }
        return emptyMap();
    }

    private Map<String, String> findRequestHeaders(ResourceConfig resourceConfig) {
        if (resourceConfig instanceof RequestHeadersResourceConfig) {
            final Map<String, String> headers = ((RequestHeadersResourceConfig) resourceConfig).getRequestHeaders();
            return ofNullable(headers).orElse(emptyMap());
        }
        return emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<ResponseConfigHolder> matchResourceConfig(
            List<ResolvedResourceConfig> resources,
            HttpMethod method,
            String pathTemplate,
            String path,
            Map<String, String> pathParams,
            Map<String, String> queryParams,
            Map<String, String> requestHeaders,
            Supplier<String> bodySupplier
    ) {
        final ResourceMethod resourceMethod = ResourceUtil.convertMethodFromVertx(method);

        List<ResolvedResourceConfig> resourceConfigs = resources.stream()
                .filter(res -> isRequestMatch(res, resourceMethod, pathTemplate, path, pathParams, queryParams, requestHeaders, bodySupplier))
                .collect(Collectors.toList());

        // find the most specific, by filtering those that match by those that specify parameters
        resourceConfigs = filterByPairs(resourceConfigs, ResolvedResourceConfig::getPathParams);
        resourceConfigs = filterByPairs(resourceConfigs, ResolvedResourceConfig::getQueryParams);
        resourceConfigs = filterByPairs(resourceConfigs, ResolvedResourceConfig::getRequestHeaders);

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

    private List<ResolvedResourceConfig> filterByPairs(
            List<ResolvedResourceConfig> resourceConfigs,
            Function<ResolvedResourceConfig, Map<String, String>> pairsSupplier
    ) {
        final List<ResolvedResourceConfig> configsWithPairs = resourceConfigs.stream()
                .filter(res -> !pairsSupplier.apply(res).isEmpty())
                .collect(Collectors.toList());

        if (configsWithPairs.isEmpty()) {
            // no resource configs specified params - don't filter
            return resourceConfigs;
        } else {
            return configsWithPairs;
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
     * @param requestHeaders the headers of the current request
     * @param bodySupplier   supplies the request body
     * @return {@code true} if the resource matches the request, otherwise {@code false}
     */
    private boolean isRequestMatch(
            ResolvedResourceConfig resource,
            ResourceMethod resourceMethod,
            String pathTemplate,
            String path,
            Map<String, String> pathParams,
            Map<String, String> queryParams,
            Map<String, String> requestHeaders,
            Supplier<String> bodySupplier
    ) {
        final RestResourceConfig resourceConfig = resource.getConfig();

        // path template can be null when a regex route is used
        final boolean pathMatch = path.equals(resourceConfig.getPath()) ||
                ofNullable(pathTemplate).map(pt -> pt.equals(resourceConfig.getPath())).orElse(false);

        if (isNull(resourceConfig.getMethod())) {
            LOGGER.warn("Resource configuration for '{}' is missing HTTP method - will not correctly match response behaviour", resourceConfig.getPath());
        }

        return pathMatch &&
                resourceMethod.equals(resourceConfig.getMethod()) &&
                matchPairs(pathParams, resource.getPathParams(), true) &&
                matchPairs(queryParams, resource.getQueryParams(), true) &&
                matchPairs(requestHeaders, resource.getRequestHeaders(), false) &&
                matchRequestBody(bodySupplier, resource.getConfig().getRequestBody());
    }

    /**
     * If the resource contains parameter configuration, check they are all present.
     * If the configuration contains no parameters, then this evaluates to true.
     * Additional parameters not in the configuration are ignored.
     *
     * @param resourceMap           the configured parameters to match
     * @param requestMap            the parameters from the request (e.g. query or path)
     * @param caseSensitiveKeyMatch whether to match keys case-sensitively
     * @return {@code true} if the configured parameters match the request, otherwise {@code false}
     */
    private boolean matchPairs(
            Map<String, String> requestMap,
            Map<String, String> resourceMap,
            boolean caseSensitiveKeyMatch
    ) {
        // none configured - implies any match
        if (resourceMap.isEmpty()) {
            return true;
        }

        final Map<String, String> comparisonMap = caseSensitiveKeyMatch ?
                requestMap :
                CollectionUtil.convertKeysToLowerCase(requestMap);

        return resourceMap.entrySet().stream().allMatch(keyValueConfig -> {
            final String configKey = caseSensitiveKeyMatch ? keyValueConfig.getKey() : keyValueConfig.getKey().toLowerCase();
            return StringUtil.safeEquals(comparisonMap.get(configKey), keyValueConfig.getValue());
        });
    }

    /**
     * Match the request body against the supplied configuration.
     *
     * @param bodySupplier      supplies the request body
     * @param requestBodyConfig the match configuration
     * @return {@code true} if the configuration is empty, or the request body matches the configuration, otherwise {@code false}
     */
    private boolean matchRequestBody(Supplier<String> bodySupplier, RequestBodyConfig requestBodyConfig) {
        // none configured - implies any match
        if (isNull(requestBodyConfig) || Strings.isNullOrEmpty(requestBodyConfig.getJsonPath())) {
            return true;
        }
        final String body = bodySupplier.get();

        Object bodyValue;
        if (Strings.isNullOrEmpty(body)) {
            bodyValue = null;
        } else {
            try {
                bodyValue = JsonPath.read(body, requestBodyConfig.getJsonPath());
            } catch (PathNotFoundException ignored) {
                bodyValue = null;
            }
        }
        return StringUtil.safeEquals(requestBodyConfig.getValue(), bodyValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Handler<RoutingContext> handleRoute(
            ImposterConfig imposterConfig,
            List<? extends PluginConfig> allPluginConfigs,
            Vertx vertx,
            Consumer<RoutingContext> routingContextConsumer
    ) {
        final PluginConfig selectedConfig = securityService.findConfigPreferringSecurityPolicy(allPluginConfigs);
        return handleRoute(imposterConfig, selectedConfig, vertx, routingContextConsumer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Handler<RoutingContext> handleRoute(
            ImposterConfig imposterConfig,
            PluginConfig pluginConfig,
            Vertx vertx,
            Consumer<RoutingContext> routingContextConsumer
    ) {
        final List<ResolvedResourceConfig> resolvedResourceConfigs = resolveResourceConfigs(pluginConfig);

        switch (imposterConfig.getRequestHandlingMode()) {
            case SYNC:
                return routingContext -> {
                    try {
                        handleResource(pluginConfig, routingContextConsumer, routingContext, resolvedResourceConfigs);
                    } catch (Exception e) {
                        handleFailure(routingContext, e);
                    }
                };

            case ASYNC:
                return routingContext -> {
                    final Handler<Future<Object>> handler = future -> {
                        try {
                            handleResource(pluginConfig, routingContextConsumer, routingContext, resolvedResourceConfigs);
                            future.complete();
                        } catch (Exception e) {
                            future.fail(e);
                        }
                    };

                    // explicitly disable ordered execution - responses should not block each other
                    // as this causes head of line blocking performance issues
                    vertx.getOrCreateContext().executeBlocking(handler, false, result -> {
                        if (result.failed()) {
                            handleFailure(routingContext, result.cause());
                        }
                    });
                };

            default:
                throw new UnsupportedOperationException("Unsupported request handling mode: " + imposterConfig.getRequestHandlingMode());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Handler<RoutingContext> passthroughRoute(ImposterConfig imposterConfig, List<? extends PluginConfig> allPluginConfigs, Vertx vertx, Handler<RoutingContext> routingContextHandler) {
        final PluginConfig selectedConfig = securityService.findConfigPreferringSecurityPolicy(allPluginConfigs);
        return handleRoute(imposterConfig, selectedConfig, vertx, routingContextHandler::handle);
    }

    private void handleResource(
            PluginConfig pluginConfig,
            Consumer<RoutingContext> routingContextConsumer,
            RoutingContext routingContext,
            List<ResolvedResourceConfig> resolvedResourceConfigs
    ) {
        // every request has a unique ID
        final String requestId = UUID.randomUUID().toString();
        routingContext.put(ResourceUtil.RC_REQUEST_ID_KEY, requestId);

        final HttpServerResponse response = routingContext.response();
        response.putHeader("X-Imposter-Request", requestId);
        response.putHeader("Server", "imposter");

        final ResponseConfigHolder rootResourceConfig = (ResponseConfigHolder) pluginConfig;

        final HttpServerRequest request = routingContext.request();
        final ResponseConfigHolder resourceConfig = matchResourceConfig(
                resolvedResourceConfigs,
                request.method(),
                routingContext.currentRoute().getPath(),
                request.path(),
                routingContext.pathParams(),
                convertMultiMapToHashMap(request.params()),
                convertMultiMapToHashMap(request.headers()),
                routingContext::getBodyAsString
        ).orElse(rootResourceConfig);

        // allows plugins to customise behaviour
        routingContext.put(ResourceUtil.RESPONSE_CONFIG_HOLDER_KEY, resourceConfig);

        if (lifecycleHooks.allMatch(listener -> listener.isRequestPermitted(rootResourceConfig, resourceConfig, resolvedResourceConfigs, routingContext))) {
            // request is permitted to continue
            try {
                routingContextConsumer.accept(routingContext);
            } finally {
                // always perform tidy up once handled, regardless of outcome
                lifecycleHooks.forEach(listener -> listener.afterRoutingContextHandled(routingContext));
            }
        }
    }

    private void handleFailure(RoutingContext routingContext, Throwable e) {
        routingContext.fail(new RuntimeException(String.format("Unhandled exception processing %s request %s",
                routingContext.request().method(), routingContext.request().absoluteURI()), e));
    }
}
