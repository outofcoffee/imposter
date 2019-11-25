package io.gatehill.imposter.script

import io.vertx.ext.web.RoutingContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import static java.util.Collections.unmodifiableMap
import static java.util.Optional.ofNullable

/**
 * Convenience methods for script execution.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class ScriptUtil {
    private static final Logger LOGGER = LogManager.getLogger(ScriptUtil)

    /**
     * Build the {@code context} {@link Map}, containing lazily-evaluated values.
     *
     * @param routingContext
     * @param additionalContext
     * @return the context
     */
    static Map<String, Object> buildContext(RoutingContext routingContext, Map<String, Object> additionalContext) {
        def lazyParams = { key ->
            if ("params" == key) {
                routingContext.request().params().entries().collectEntries { entry -> [entry.key, entry.value] }
            } else {
                null
            }
        }

        def deprecatedParams = {
            if ("params" == it) {
                LOGGER.warn("Deprecation notice: 'context.params' is deprecated and will be removed " +
                        "in a future version. Use 'context.request.params' instead.")
            }
            lazyParams(it)
        }

        def deprecatedUri = {
            LOGGER.warn("Deprecation notice: 'context.uri' is deprecated and will be removed " +
                    "in a future version. Use 'context.request.uri' instead.")
            routingContext.request().absoluteURI()
        }

        // root context
        // NOTE: params and uri present for legacy script support
        final Map<String, Object> context = [:].withDefault deprecatedParams
        context.put "uri", "${-> deprecatedUri()}"

        // request information
        final Map<String, Object> request = [:].withDefault lazyParams
        request.put "method", "${-> routingContext.request().method().name()}"
        request.put "uri", "${-> routingContext.request().absoluteURI()}"
        request.put "body", "${-> routingContext.getBodyAsString()}"
        request.put "headers", routingContext.request().headers()
        context.put "request", unmodifiableMap(request)

        // additional context
        ofNullable(additionalContext).ifPresent({ additional -> context.putAll(additional) })

        return unmodifiableMap(context)
    }
}
