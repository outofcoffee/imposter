package io.gatehill.imposter.plugin;

import io.vertx.ext.web.Router;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface Plugin extends PluginMetadata {
    void configureRoutes(Router router);
}
