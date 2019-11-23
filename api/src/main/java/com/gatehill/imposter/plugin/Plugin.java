package com.gatehill.imposter.plugin;

import io.vertx.ext.web.Router;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface Plugin {
    void configureRoutes(Router router);

    default String getName() {
        return ofNullable(getClass().getAnnotation(PluginInfo.class))
                .map(PluginInfo::value)
                .orElse(getClass().getCanonicalName());
    }
}
