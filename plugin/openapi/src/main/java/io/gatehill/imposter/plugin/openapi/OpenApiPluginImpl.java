/*
 * Copyright (c) 2016-2021.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.gatehill.imposter.plugin.openapi;

import com.google.common.collect.Lists;
import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.http.StatusCodeFactory;
import io.gatehill.imposter.plugin.PluginInfo;
import io.gatehill.imposter.plugin.RequireModules;
import io.gatehill.imposter.plugin.ScriptedPlugin;
import io.gatehill.imposter.plugin.config.ConfiguredPlugin;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.plugin.openapi.config.OpenApiPluginConfig;
import io.gatehill.imposter.plugin.openapi.http.OpenApiResponseBehaviourFactory;
import io.gatehill.imposter.plugin.openapi.loader.SpecificationLoader;
import io.gatehill.imposter.plugin.openapi.service.ExampleService;
import io.gatehill.imposter.plugin.openapi.service.SpecificationService;
import io.gatehill.imposter.script.ResponseBehaviour;
import io.gatehill.imposter.service.ResourceService;
import io.gatehill.imposter.service.ResponseService;
import io.gatehill.imposter.util.HttpUtil;
import io.gatehill.imposter.util.LogUtil;
import io.gatehill.imposter.util.MapUtil;
import io.gatehill.imposter.util.ResourceUtil;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

/**
 * Plugin for OpenAPI (OAI; formerly known as 'Swagger').
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@PluginInfo("openapi")
@RequireModules(OpenApiModule.class)
public class OpenApiPluginImpl extends ConfiguredPlugin<OpenApiPluginConfig> implements ScriptedPlugin<OpenApiPluginConfig> {
    private static final Logger LOGGER = LogManager.getLogger(OpenApiPluginImpl.class);
    private static final String UI_WEB_ROOT = "swagger-ui";

    /**
     * 'default' is a special case in OpenAPI that does not have a status code.
     */
    private static final String DEFAULT_RESPONSE_KEY = "default";

    static final String SPECIFICATION_PATH = "/_spec";
    static final String COMBINED_SPECIFICATION_PATH = SPECIFICATION_PATH + "/combined.json";

    @Inject
    private ImposterConfig imposterConfig;

    @Inject
    private ResourceService resourceService;

    @Inject
    private SpecificationService specificationService;

    @Inject
    private ExampleService exampleService;

    @Inject
    private ResponseService responseService;

    @Inject
    private OpenApiResponseBehaviourFactory openApiResponseBehaviourFactory;

    private List<OpenApiPluginConfig> configs;
    private List<OpenAPI> allSpecs;

    @Override
    protected Class<OpenApiPluginConfig> getConfigClass() {
        return OpenApiPluginConfig.class;
    }

    @Override
    protected void configurePlugin(List<OpenApiPluginConfig> configs) {
        this.configs = configs;
    }

    static {
        MapUtil.addJavaTimeSupport(io.swagger.util.Json.mapper());
    }

    @Override
    public void configureRoutes(Router router) {
        parseSpecs(router);

        // serve specification and UI
        LOGGER.debug("Adding specification UI at: {}{}", imposterConfig.getServerUrl(), SPECIFICATION_PATH);
        router.get(COMBINED_SPECIFICATION_PATH).handler(resourceService.handleRoute(imposterConfig, configs, vertx, this::handleCombinedSpec));
        router.getWithRegex(SPECIFICATION_PATH + "$").handler(resourceService.handleRoute(imposterConfig, configs, vertx, routingContext -> routingContext.response().putHeader("Location", SPECIFICATION_PATH + "/").setStatusCode(HttpUtil.HTTP_MOVED_PERM).end()));
        router.get(SPECIFICATION_PATH + "/*").handler(StaticHandler.create(UI_WEB_ROOT));
    }

    private void parseSpecs(Router router) {
        allSpecs = Lists.newArrayListWithExpectedSize(configs.size());

        // specification mock endpoints
        configs.forEach(config -> {
            final OpenAPI spec = SpecificationLoader.parseSpecification(config);

            if (null != spec) {
                allSpecs.add(spec);
                spec.getPaths().forEach((path, pathConfig) ->
                        handlePathOperations(router, config, spec, path, pathConfig)
                );

            } else {
                throw new RuntimeException(String.format("Unable to load API specification: %s", config.getSpecFile()));
            }
        });
    }

    /**
     * Bind a handler to each operation.
     *
     * @param router     the Vert.x router
     * @param config     the plugin configuration
     * @param spec       the OpenAPI specification
     * @param path       the mock path
     * @param pathConfig the path configuration
     */
    private void handlePathOperations(Router router, OpenApiPluginConfig config, OpenAPI spec, String path, PathItem pathConfig) {
        pathConfig.readOperationsMap().forEach((httpMethod, operation) -> {
            final String fullPath = buildFullPath(buildBasePath(config, spec), path);
            LOGGER.debug("Adding mock endpoint: {} -> {}", httpMethod, fullPath);

            // convert an io.swagger.models.HttpMethod to an io.vertx.core.http.HttpMethod
            final HttpMethod method = HttpMethod.valueOf(httpMethod.name());
            router.route(method, fullPath).handler(buildHandler(config, operation, spec));
        });
    }

    /**
     * Construct the full path from the base path and the operation path.
     *
     * @param basePath          the base path
     * @param specOperationPath the operation path from the OpenAPI specification
     * @return the full path
     */
    private String buildFullPath(String basePath, String specOperationPath) {
        final String operationPath = ResourceUtil.convertPathToVertx(specOperationPath);
        if (basePath.endsWith("/")) {
            if (operationPath.startsWith("/")) {
                return basePath + operationPath.substring(1);
            } else {
                return basePath + operationPath;
            }
        } else {
            if (operationPath.startsWith("/")) {
                return basePath + operationPath;
            } else {
                return basePath + "/" + operationPath;
            }
        }
    }

    /**
     * Construct the base path, optionally dependent on the server path,
     * from which the example response will be served.
     *
     * @param config the mock configuration
     * @param spec   the OpenAPI specification
     * @return the base path
     */
    private String buildBasePath(OpenApiPluginConfig config, OpenAPI spec) {
        if (config.isUseServerPathAsBaseUrl()) {
            // Treat the mock server as substitute for 'the' server.
            // Note: OASv2 'basePath' is converted to OASv3 'server' entries.

            final Optional<Server> firstServer = spec.getServers().stream().findFirst();
            if (firstServer.isPresent()) {
                final String url = ofNullable(firstServer.get().getUrl()).orElse("");
                if (url.length() > 1) {
                    // attempt to parse as URI and extract path
                    try {
                        return new URI(url).getPath();
                    } catch (URISyntaxException ignored) {
                    }
                }
            }
        }
        return "";
    }

    /**
     * Returns an OpenAPI specification combining all the given specifications.
     *
     * @param routingContext the Vert.x routing context
     */
    private void handleCombinedSpec(RoutingContext routingContext) {
        try {
            routingContext.response()
                    .putHeader(HttpUtil.CONTENT_TYPE, HttpUtil.CONTENT_TYPE_JSON)
                    .end(specificationService.getCombinedSpecSerialised(imposterConfig, allSpecs));

        } catch (Exception e) {
            routingContext.fail(e);
        }
    }

    /**
     * Build a handler for the given operation.
     *
     * @param pluginConfig the plugin configuration
     * @param operation    the specification operation
     * @param spec         the OpenAPI specification
     * @return a route handler
     */
    private Handler<RoutingContext> buildHandler(OpenApiPluginConfig pluginConfig, Operation operation, OpenAPI spec) {
        // statically calculate as much as possible
        final StatusCodeFactory statusCodeFactory = buildStatusCodeCalculator(operation);

        return resourceService.handleRoute(imposterConfig, pluginConfig, vertx, routingContext -> {
            if (!specificationService.isValidRequest(imposterConfig, pluginConfig, routingContext, allSpecs)) {
                return;
            }

            final Map<String, Object> context = newHashMap();
            context.put("operation", operation);

            final HttpServerRequest request = routingContext.request();
            final ResponseConfigHolder resourceConfig = routingContext.get(ResourceUtil.RESPONSE_CONFIG_HOLDER_KEY);

            final Consumer<ResponseBehaviour> defaultBehaviourHandler = responseBehaviour -> {
                final Optional<ApiResponse> optionalResponse = findApiResponse(operation, responseBehaviour.getStatusCode());

                // set status code regardless of response strategy
                final HttpServerResponse response = routingContext.response()
                        .setStatusCode(responseBehaviour.getStatusCode());

                if (optionalResponse.isPresent()) {
                    if (!responseBehaviour.getResponseHeaders().containsKey(HttpUtil.CONTENT_TYPE)) {
                        setContentTypeFromSpec(routingContext, responseBehaviour, optionalResponse);
                    }

                    // build a response from the specification
                    final ResponseService.ResponseSender exampleSender = (rc, rb) ->
                            exampleService.serveExample(imposterConfig, pluginConfig, rc, rb, optionalResponse.get(), spec);

                    // attempt to serve an example from the specification, falling back if not present
                    responseService.sendResponse(
                            pluginConfig, resourceConfig, routingContext, responseBehaviour, exampleSender, this::fallback);

                } else {
                    LOGGER.warn("No response found in specification for {} {} and status code {}",
                            request.method(),
                            request.path(),
                            responseBehaviour.getStatusCode()
                    );

                    response.end();
                }
            };

            scriptHandler(
                    pluginConfig,
                    resourceConfig,
                    routingContext,
                    getInjector(),
                    context,
                    statusCodeFactory,
                    openApiResponseBehaviourFactory,
                    defaultBehaviourHandler
            );
        });
    }

    private void setContentTypeFromSpec(RoutingContext routingContext, ResponseBehaviour responseBehaviour, Optional<ApiResponse> optionalResponse) {
        ofNullable(optionalResponse.get().getContent()).ifPresent(responseContent -> {
            final Optional<String> firstContentType = responseContent.keySet().stream().findFirst();
            switch (responseContent.size()) {
                case 0:
                    return;
                case 1:
                    LOGGER.debug(
                            "Setting content type [{}] from specification for {}",
                            firstContentType.get(),
                            LogUtil.describeRequestShort(routingContext)
                    );
                    break;
                default:
                    LOGGER.warn(
                            "Multiple content types in specification - selecting first [{}] for {}",
                            firstContentType.get(),
                            LogUtil.describeRequestShort(routingContext)
                    );
                    break;
            }
            responseBehaviour.getResponseHeaders().put(HttpUtil.CONTENT_TYPE, firstContentType.get());
        });
    }

    private StatusCodeFactory buildStatusCodeCalculator(Operation operation) {
        return rc -> {
            if (nonNull(rc.getResponseConfig().getStatusCode())) {
                return rc.getResponseConfig().getStatusCode();
            } else {
                // Choose the first response for this operation.
                // Note: responses are keyed on their status code.
                final ApiResponses responses = operation.getResponses();
                if (nonNull(responses) && !responses.isEmpty()) {
                    final String firstStatus = responses.keySet().iterator().next();

                    // default is a special case in OpenAPI that does not have a status code
                    if (!DEFAULT_RESPONSE_KEY.equalsIgnoreCase(firstStatus)) {
                        return Integer.parseInt(firstStatus);
                    }
                }
            }
            return HttpUtil.HTTP_OK;
        };
    }

    private Optional<ApiResponse> findApiResponse(Operation operation, Integer statusCode) {
        // openapi statuses are represented as strings
        final String status = String.valueOf(statusCode);

        // look for a specification response based on the status code
        final Optional<ApiResponse> optionalResponse = operation.getResponses().entrySet().parallelStream()
                .filter(mockResponse -> mockResponse.getKey().equals(status))
                .map(Map.Entry::getValue)
                .findAny();

        if (optionalResponse.isPresent()) {
            return optionalResponse;
        } else {
            // fall back to default
            LOGGER.debug("No response found for status code {}; falling back to default response if present", statusCode);
            return ofNullable(operation.getResponses().getDefault());
        }
    }

    /**
     * Handles the scenario when no example is found.
     *
     * @param routingContext    the Vert.x routing context
     * @param responseBehaviour the response behaviour
     */
    private boolean fallback(RoutingContext routingContext, ResponseBehaviour responseBehaviour) {
        LOGGER.warn("No example match found and no response file set for mock response for URI {} with status code {}" +
                " - sending empty response", routingContext.request().absoluteURI(), responseBehaviour.getStatusCode());

        routingContext.response().end();
        return true;
    }
}
