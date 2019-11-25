package io.gatehill.imposter.plugin;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@Retention(RUNTIME)
@Target({ElementType.TYPE})
public @interface PluginInfo {
    /**
     * @return the plugin short name
     */
    String value();
}
