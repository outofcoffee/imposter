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
    private int statusCode;
    private boolean handled;
    private Map<String, String> params;

    private InvocationContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    public static InvocationContext build(RoutingContext routingContext) {
        return new InvocationContext(routingContext);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("uri", getUri())
                .add("statusCode", statusCode)
                .add("handled", handled)
                .toString();
    }

    public void respondWithStatusCode(int statusCode) {
        handled = true;
        this.statusCode = statusCode;
    }

    public void respondDefault() {
        handled = false;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isHandled() {
        return handled;
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
}
