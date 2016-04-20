package com.gatehill.imposter.model;

import com.google.common.base.Objects;
import io.vertx.ext.web.RoutingContext;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class InvocationContext {
    private RoutingContext routingContext;
    private Map<String, String> params;
    private Map<String, Object> additional;

    private InvocationContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    public static InvocationContext build(RoutingContext routingContext, Map<String, Object> additionalContext) {
        final InvocationContext context = new InvocationContext(routingContext);
        context.setAdditional(additionalContext);
        return context;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
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

    public Map<String, Object> getAdditional() {
        return additional;
    }

    public void setAdditional(Map<String, Object> additional) {
        this.additional = additional;
    }
}
