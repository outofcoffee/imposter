package com.gatehill.imposter.model;

import com.google.common.base.Objects;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InvocationContext {
    private final String scriptName;
    private final RoutingContext routingContext;
    private Map<String, String> params;
    private Map<String, Object> additional;

    private InvocationContext(String scriptName, RoutingContext routingContext) {
        this.scriptName = scriptName;
        this.routingContext = routingContext;
    }

    public static InvocationContext build(String scriptName, RoutingContext routingContext, Map<String, Object> additionalContext) {
        final InvocationContext context = new InvocationContext(scriptName, routingContext);
        context.setAdditional(additionalContext);
        return context;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("scriptName", scriptName)
                .add("method", getMethod())
                .add("uri", getUri())
                .add("params", getParams())
                .toString();
    }

    public String getUri() {
        return routingContext.request().absoluteURI();
    }

    public Map<String, String> getParams() {
        if (null == params) {
            params = routingContext.request().params().entries().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return params;
    }

    Map<String, Object> getAdditional() {
        return additional;
    }

    private void setAdditional(Map<String, Object> additional) {
        this.additional = additional;
    }

    public Logger getLogger() {
        return LogManager.getLogger(scriptName);
    }

    public String getMethod() {
        return routingContext.request().method().name();
    }
}
