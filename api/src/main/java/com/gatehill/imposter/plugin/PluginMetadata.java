package com.gatehill.imposter.plugin;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
interface PluginMetadata {
    default String getName() {
        return getPluginName(getClass());
    }

    static String getPluginName(Class<? extends PluginMetadata> clazz) {
        return ofNullable(clazz.getAnnotation(PluginInfo.class))
                .map(PluginInfo::value)
                .orElse(clazz.getCanonicalName());
    }
}
