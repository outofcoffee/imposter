package io.gatehill.imposter.lifecycle;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class ImposterLifecycleHooks {
    private static final Logger LOGGER = LogManager.getLogger(ImposterLifecycleHooks.class);

    private final List<ImposterLifecycleListener> listeners = newArrayList();

    public void registerListener(ImposterLifecycleListener listener) {
        LOGGER.trace("Registered listener: {}", listener.getClass().getCanonicalName());
        this.listeners.add(listener);
    }

    public void forEach(Consumer<ImposterLifecycleListener> listenerConsumer) {
        if (listeners.isEmpty()) {
            return;
        }
        listeners.forEach(listenerConsumer);
    }

    public boolean allMatch(Predicate<ImposterLifecycleListener> listenerConsumer) {
        if (listeners.isEmpty()) {
            return true;
        }
        return listeners.stream().allMatch(listenerConsumer);
    }

    public boolean isEmpty() {
        return listeners.isEmpty();
    }
}
