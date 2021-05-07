package io.gatehill.imposter.script

import io.vertx.ext.web.RoutingContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import static java.util.Optional.ofNullable

/**
 * Convenience methods for script execution.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class ScriptUtil {
    private static final Logger LOGGER = LogManager.getLogger(ScriptUtil)

    /**
     * Build the {@code context}, containing lazily-evaluated values.
     *
     * @param routingContext
     * @param additionalContext
     * @return the context
     */
    static ExecutionContext buildContext(RoutingContext routingContext, Map<String, Object> additionalContext) {
        final Map<String, String> params = routingContext.request().params().entries().collectEntries()
        final Map<String, String> headers = routingContext.request().headers().collectEntries()

        def deprecatedParams = {
            LOGGER.warn("Deprecation notice: 'context.params' is deprecated and will be removed " +
                    "in a future version. Use 'context.request.params' instead.")
            params
        }

        def deprecatedUri = {
            LOGGER.warn("Deprecation notice: 'context.uri' is deprecated and will be removed " +
                    "in a future version. Use 'context.request.uri' instead.")
            routingContext.request().absoluteURI()
        }

        // root context
        def executionContext = new ExecutionContext()

        // NOTE: params and uri present for legacy script support
        executionContext.metaClass.getParams = deprecatedParams
        executionContext.metaClass.uri = "${-> deprecatedUri()}"

        // request information
        def request = new ExecutionContext.Request()
        request.method = "${-> routingContext.request().method().name()}"
        request.uri = "${-> routingContext.request().absoluteURI()}"
        request.body = "${-> routingContext.getBodyAsString()}"
        request.headers = headers
        request.params = params
        executionContext.request = request

        // additional context
        ofNullable(additionalContext).ifPresent({ additional ->
            additional.each { executionContext.metaClass[it.key] = it.value }
        })

        return executionContext
    }
}
