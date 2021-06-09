package io.gatehill.imposter.service;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface ResourceService {
    Handler<RoutingContext> handleRoute(
            ImposterConfig imposterConfig,
            List<? extends PluginConfig> allPluginConfigs,
            Vertx vertx,
            Consumer<RoutingContext> routingContextConsumer
    );

    Handler<RoutingContext> handleRoute(
            ImposterConfig imposterConfig,
            PluginConfig pluginConfig,
            Vertx vertx,
            Consumer<RoutingContext> routingContextConsumer
    );
}
