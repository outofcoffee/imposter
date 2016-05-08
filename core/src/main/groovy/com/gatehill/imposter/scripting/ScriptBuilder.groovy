package com.gatehill.imposter.scripting

import io.vertx.ext.web.RoutingContext

import java.util.stream.Collectors

import static java.util.Collections.unmodifiableMap
import static java.util.Optional.ofNullable

/**
 * Builds a script, for execution within a Groovy environment.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
class ScriptBuilder {
    private static final String PREFIX_IMPORT = "import "
    private static final String NEW_LINE = System.properties["line.separator"]

    /**
     * Builds an executable script, from a template and the given content.
     *
     * @param scriptContent the content
     * @return the script
     */
    static String buildScript(List<String> scriptContent) {
        return """
            import com.gatehill.imposter.model.ResponseBehaviour
            ${getImports(scriptContent)}

            def __responseBehaviour = new ResponseBehaviour() {
                public void process() throws Exception {
                    ${getNonImportStatements(scriptContent)}
                }
            }

            __responseHolder.set(__responseBehaviour)
            __responseBehaviour.process()
        """
    }

    /**
     * @param scriptContent the content
     * @return the import statements
     */
    private static String getImports(List<String> scriptContent) {
        return scriptContent.stream()
                .filter({ line -> line.startsWith(PREFIX_IMPORT) })
                .collect(Collectors.joining(NEW_LINE))
    }

    /**
     * @param scriptContent the content
     * @return the lines of the script excluding the import statements
     */
    private static String getNonImportStatements(List<String> scriptContent) {
        return scriptContent.stream()
                .filter({ line -> !line.startsWith(PREFIX_IMPORT) })
                .collect(Collectors.joining(NEW_LINE))
    }

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

        // root context
        Map<String, Object> context = [:]

        // legacy script support:
        // <--
        context = context.withDefault lazyParams
        context.put "uri", { -> routingContext.request().absoluteURI() }
        // -->

        final Map<String, Object> request = [:].withDefault lazyParams
        request.put("method", "${-> routingContext.request().method().name()}")
        request.put("uri", "${-> routingContext.request().absoluteURI()}")
        request.put("body", "${-> routingContext.getBodyAsString()}")

        // request is read-only
        context.put("request", unmodifiableMap(request))

        // additional context
        ofNullable(additionalContext).ifPresent({ additional -> context.putAll(additional) })

        // context is read-only
        return unmodifiableMap(context)
    }
}
