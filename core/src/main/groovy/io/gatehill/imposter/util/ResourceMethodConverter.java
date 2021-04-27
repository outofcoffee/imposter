package io.gatehill.imposter.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.gatehill.imposter.plugin.config.ContentTypedConfig;
import io.gatehill.imposter.plugin.config.resource.MethodResourceConfig;
import io.gatehill.imposter.plugin.config.resource.ResourceMethod;
import io.vertx.core.http.HttpMethod;

import static java.util.Optional.ofNullable;

/**
 * Converts {@link ResourceMethod}s to {@link HttpMethod}s.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public final class ResourceMethodConverter {
    private static final BiMap<ResourceMethod, HttpMethod> METHODS = HashBiMap.create();

    static {
        METHODS.put(ResourceMethod.GET, HttpMethod.GET);
        METHODS.put(ResourceMethod.HEAD, HttpMethod.HEAD);
        METHODS.put(ResourceMethod.POST, HttpMethod.POST);
        METHODS.put(ResourceMethod.PUT, HttpMethod.PUT);
        METHODS.put(ResourceMethod.PATCH, HttpMethod.PATCH);
        METHODS.put(ResourceMethod.DELETE, HttpMethod.DELETE);
        METHODS.put(ResourceMethod.CONNECT, HttpMethod.CONNECT);
        METHODS.put(ResourceMethod.OPTIONS, HttpMethod.OPTIONS);
        METHODS.put(ResourceMethod.TRACE, HttpMethod.TRACE);
    }

    public static HttpMethod convertMethodToVertx(ContentTypedConfig resourceConfig) {
        if (resourceConfig instanceof MethodResourceConfig) {
            final ResourceMethod method = ofNullable(((MethodResourceConfig) resourceConfig).getMethod())
                    .orElse(ResourceMethod.GET);

            return ofNullable(METHODS.get(method))
                    .orElseThrow(() -> new UnsupportedOperationException("Unknown method: " + method));
        } else {
            return HttpMethod.GET;
        }
    }

    public static ResourceMethod convertMethodFromVertx(HttpMethod method) {
        return ofNullable(METHODS.inverse().get(method))
                .orElseThrow(() -> new UnsupportedOperationException("Unknown method: " + method));
    }
}
