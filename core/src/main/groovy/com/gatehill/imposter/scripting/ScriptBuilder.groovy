package com.gatehill.imposter.scripting

import java.util.stream.Collectors

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
            __responseBehaviour.setInvocationContext(context)
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
}
