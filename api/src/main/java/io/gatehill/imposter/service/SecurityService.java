package io.gatehill.imposter.service;

import io.gatehill.imposter.ImposterConfig;
import io.gatehill.imposter.plugin.config.PluginConfig;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface SecurityService {
    void enforce(ImposterConfig imposterConfig, Vertx vertx, Router router, PluginConfig config);
}
