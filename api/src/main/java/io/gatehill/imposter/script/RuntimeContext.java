package io.gatehill.imposter.script;

import com.google.common.collect.Maps;
import io.gatehill.imposter.plugin.config.PluginConfig;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static java.util.Optional.ofNullable;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class RuntimeContext {
    private Logger logger;
    private PluginConfig pluginConfig;
    private Map<String, Object> additionalBindings;
    private ExecutionContext executionContext;

    public RuntimeContext(Logger logger, PluginConfig pluginConfig, Map<String, Object> additionalBindings, ExecutionContext executionContext) {
        this.logger = logger;
        this.pluginConfig = pluginConfig;
        this.additionalBindings = additionalBindings;
        this.executionContext = executionContext;
    }

    /**
     * @return a representation of the runtime context as a {@link Map} of bindings
     */
    public Map<String, Object> asMap() {
        final Map<String, Object> bindings = Maps.newHashMap();
        bindings.put("logger", logger);
        bindings.put("config", pluginConfig);
        bindings.put("context", executionContext);

        // add custom bindings
        ofNullable(additionalBindings).ifPresent(bindings::putAll);

        return bindings;
    }

    public Logger getLogger() {
        return logger;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public Map<String, Object> getAdditionalBindings() {
        return additionalBindings;
    }

    public ExecutionContext getExecutionContext() {
        return executionContext;
    }
}
