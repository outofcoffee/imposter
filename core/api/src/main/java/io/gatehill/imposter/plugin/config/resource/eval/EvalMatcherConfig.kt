package io.gatehill.imposter.plugin.config.resource.eval

import com.fasterxml.jackson.annotation.JsonProperty
import io.gatehill.imposter.plugin.config.resource.conditional.MatchOperator

/**
 * Configuration for an eval matcher.
 */
data class EvalMatcherConfig(
    /**
     * The expression to evaluate using Imposter's template syntax.
     */
    @field:JsonProperty("expression")
    var expression: String? = null,

    /**
     * The operator to use for matching.
     * Defaults to EqualTo if not specified.
     */
    @field:JsonProperty("operator")
    var operator: MatchOperator? = null,

    /**
     * The value to match against.
     */
    @field:JsonProperty("value")
    var value: String? = null,
)
