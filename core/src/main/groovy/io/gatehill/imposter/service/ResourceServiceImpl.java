package io.gatehill.imposter.service;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.config.ResolvedResourceConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.gatehill.imposter.plugin.config.PluginConfigImpl;
import io.gatehill.imposter.plugin.config.resource.ResponseConfigHolder;
import io.gatehill.imposter.plugin.config.security.SecurityConfig;
import io.gatehill.imposter.plugin.config.security.SecurityConfigHolder;
import io.gatehill.imposter.util.ResourceUtil;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.gatehill.imposter.util.HttpUtil.convertMultiMapToHashMap;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * @author pete
 */
public class ResourceServiceImpl implements ResourceService {
    private static final Logger LOGGER = LogManager.getLogger(ResourceServiceImpl.class);

    @Inject
    private SecurityService securityService;

    @Inject
    private ResponseService responseService;

    @Override
    public Handler<RoutingContext> handleRoute(
            ImposterConfig imposterConfig,
            List<? extends PluginConfig> allPluginConfigs,
            Vertx vertx,
            Consumer<RoutingContext> routingContextConsumer
    ) {
        final PluginConfig selectedConfig = findConfigWithSecurityPolicyOrNone(allPluginConfigs);
        return handleRoute(imposterConfig, selectedConfig, vertx, routingContextConsumer);
    }

    private PluginConfig findConfigWithSecurityPolicyOrNone(List<? extends PluginConfig> allPluginConfigs) {
        final List<PluginConfig> configsWithSecurity = allPluginConfigs.stream().filter(c -> {
            if (c instanceof SecurityConfigHolder) {
                return nonNull(((SecurityConfigHolder) c).getSecurity());
            }
            return false;
        }).collect(Collectors.toList());

        // only zero or one configurations can specify the 'security' block
        final PluginConfig selectedConfig;
        if (configsWithSecurity.isEmpty()) {
            // TODO improve this
            selectedConfig = new PluginConfigImpl();
        } else if (configsWithSecurity.size() == 1) {
            selectedConfig = configsWithSecurity.get(0);
        } else {
            throw new IllegalStateException("Cannot specify root 'security' configuration block more than once. Ensure only one configuration file contains the root 'security' block.");
        }
        return selectedConfig;
    }

    /**
     * Builds a {@link Handler} that processes a request.
     * <p>
     * If {@code requestHandlingMode} is {@link io.gatehill.imposter.server.RequestHandlingMode#SYNC}, then the {@code routingContextConsumer}
     * is invoked on the calling thread.
     * <p>
     * If it is {@link io.gatehill.imposter.server.RequestHandlingMode#ASYNC}, then upon receiving a request,
     * the {@code routingContextConsumer} is invoked on a worker thread, passing the {@code routingContext}.
     * <p>
     * Example:
     * <pre>
     * router.get("/example").handler(handleRoute(imposterConfig, vertx, routingContext -> {
     *     // use routingContext
     * });
     * </pre>
     *
     * @param vertx                  the current Vert.x instance
     * @param routingContextConsumer the consumer of the {@link RoutingContext}
     * @return the handler
     */
    @Override
    public Handler<RoutingContext> handleRoute(
            ImposterConfig imposterConfig,
            PluginConfig pluginConfig,
            Vertx vertx,
            Consumer<RoutingContext> routingContextConsumer
    ) {
        switch (imposterConfig.getRequestHandlingMode()) {
            case SYNC:
                return routingContext -> {
                    try {
                        handleResource(pluginConfig, routingContextConsumer, routingContext);
                    } catch (Exception e) {
                        handleFailure(routingContext, e);
                    }
                };

            case ASYNC:
                return routingContext -> vertx.getOrCreateContext().executeBlocking(future -> {
                    try {
                        handleResource(pluginConfig, routingContextConsumer, routingContext);
                        future.complete();
                    } catch (Exception e) {
                        future.fail(e);
                    }

                }, result -> {
                    if (result.failed()) {
                        handleFailure(routingContext, result.cause());
                    }
                });

            default:
                throw new UnsupportedOperationException("Unsupported request handling mode: " + imposterConfig.getRequestHandlingMode());
        }
    }

    private void handleResource(PluginConfig pluginConfig, Consumer<RoutingContext> routingContextConsumer, RoutingContext routingContext) {
        if (configureResource(pluginConfig, routingContext)) {
            // request is permitted to continue
            routingContextConsumer.accept(routingContext);
        }
    }

    private void handleFailure(RoutingContext routingContext, Throwable e) {
        routingContext.fail(new RuntimeException(String.format("Unhandled exception processing %s request %s",
                routingContext.request().method(), routingContext.request().absoluteURI()), e));
    }

    /**
     *
     * @param pluginConfig
     * @param routingContext
     * @return {@code true} if the request is permitted to continue, otherwise {@code false}
     */
    private boolean configureResource(PluginConfig pluginConfig, RoutingContext routingContext) {
        final ResponseConfigHolder rootResourceConfig = (ResponseConfigHolder) pluginConfig;

        final List<ResolvedResourceConfig> resolvedResourceConfigs = responseService.resolveResourceConfigs(rootResourceConfig);

        final HttpServerRequest request = routingContext.request();
        final ResponseConfigHolder resourceConfig = responseService.matchResourceConfig(
                resolvedResourceConfigs,
                request.method(),
                routingContext.currentRoute().getPath(),
                request.path(),
                routingContext.pathParams(),
                convertMultiMapToHashMap(request.params())
        ).orElse(rootResourceConfig);

        // allows plugins to customise behaviour
        routingContext.put(ResourceUtil.RESPONSE_CONFIG_HOLDER_KEY, resourceConfig);

        final SecurityConfig security = getSecurityConfig(rootResourceConfig, resourceConfig);
        if (nonNull(security)) {
            LOGGER.trace("Enforcing security policy [{} conditions]", security.getConditions().size());
            return securityService.enforce(security, routingContext);
        } else {
            LOGGER.trace("No security policy found");
            return true;
        }
    }

    private SecurityConfig getSecurityConfig(ResponseConfigHolder rootResourceConfig, ResponseConfigHolder resourceConfig) {
        SecurityConfig security = getSecurityConfig(resourceConfig);
        if (isNull(security)) {
            // IMPORTANT: if no resource security, fall back to root security
            security = getSecurityConfig(rootResourceConfig);
        }
        return security;
    }

    private SecurityConfig getSecurityConfig(ResponseConfigHolder resourceConfig) {
        if (!(resourceConfig instanceof SecurityConfigHolder)) {
            return null;
        }
        return ((SecurityConfigHolder) resourceConfig).getSecurity();
    }
}
