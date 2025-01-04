package io.gatehill.imposter.plugin.config.resource.eval

/**
 * Holds configuration for eval matchers.
 */
interface EvalMatchersConfigHolder {
    /**
     * The list of eval matchers.
     */
    var evals: List<EvalMatcherConfig>?
}
