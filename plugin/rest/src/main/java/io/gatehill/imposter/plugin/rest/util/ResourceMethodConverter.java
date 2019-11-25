package io.gatehill.imposter.plugin.rest.util;

import io.gatehill.imposter.plugin.config.ContentTypedConfig;
import io.gatehill.imposter.plugin.rest.config.MethodResourceConfig;
import io.gatehill.imposter.plugin.rest.config.ResourceMethod;
import io.vertx.core.http.HttpMethod;

import static java.util.Optional.ofNullable;

/**
 * Converts {@link ResourceMethod}s to {@link HttpMethod}s.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class ResourceMethodConverter {
    public static HttpMethod convertMethod(ContentTypedConfig resourceConfig) {
        if (resourceConfig instanceof MethodResourceConfig) {
            final ResourceMethod method = ofNullable(((MethodResourceConfig) resourceConfig).getMethod()).orElse(ResourceMethod.GET);
            switch (method) {
                case GET:
                    return HttpMethod.GET;
                case HEAD:
                    return HttpMethod.HEAD;
                case POST:
                    return HttpMethod.POST;
                case PUT:
                    return HttpMethod.PUT;
                case DELETE:
                    return HttpMethod.DELETE;
                case CONNECT:
                    return HttpMethod.CONNECT;
                case OPTIONS:
                    return HttpMethod.OPTIONS;
                case TRACE:
                    return HttpMethod.TRACE;
                case PATCH:
                    return HttpMethod.PATCH;
                default:
                    throw new UnsupportedOperationException("Unknown method: " + method);
            }
        } else {
            return HttpMethod.GET;
        }
    }
}
