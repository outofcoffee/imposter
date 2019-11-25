package io.gatehill.imposter.util;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class InjectorUtil {
    private static final Logger LOGGER = LogManager.getLogger(InjectorUtil.class);

    private static Injector injector;

    private InjectorUtil() {
    }

    public static Injector getInjector() {
        return injector;
    }

    public static Injector create(Module... modules) {
        if (null != injector) {
            LOGGER.warn("Injector already initialised");
        }

        injector = Guice.createInjector(modules);
        return injector;
    }
}
