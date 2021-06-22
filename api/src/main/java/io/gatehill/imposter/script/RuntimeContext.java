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
    private final Map<String, String> env;
    private final Logger logger;
    private final PluginConfig pluginConfig;
    private final Map<String, Object> additionalBindings;
    private final ExecutionContext executionContext;

    public RuntimeContext(
            Map<String, String> env,
            Logger logger,
            PluginConfig pluginConfig,
            Map<String, Object> additionalBindings,
            ExecutionContext executionContext
    ) {
        this.env = env;
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
        bindings.put("config", pluginConfig);
        bindings.put("context", executionContext);
        bindings.put("env", env);
        bindings.put("logger", logger);

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
