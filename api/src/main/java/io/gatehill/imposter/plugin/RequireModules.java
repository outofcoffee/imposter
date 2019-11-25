package io.gatehill.imposter.plugin;

import com.google.inject.Module;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
@Retention(RUNTIME)
@Target({ElementType.TYPE})
public @interface RequireModules {
    /**
     * @return the dependency injection modules
     */
    Class<? extends Module>[] value();
}
