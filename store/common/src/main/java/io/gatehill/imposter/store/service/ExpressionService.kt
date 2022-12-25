package io.gatehill.imposter.store.service

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.store.service.expression.ContextEvaluator
import io.gatehill.imposter.store.service.expression.DateTimeEvaluator
import io.gatehill.imposter.store.service.expression.ExpressionEvaluator

/**
 * Evaluates expressions against the [HttpExchange].
 *
 * @author Pete Cornish
 */
interface ExpressionService {
    /**
     * Evaluates an expression in the form:
     * ```
     * ${expression}
     * ```
     * or composite expressions such as:
     * ```
     * ${expression1}...${expression2}...
     * ```
     * If no expression is found, [expression] is returned.
     */
    fun eval(expression: String, httpExchange: HttpExchange, evaluators: Map<String, ExpressionEvaluator<*>>? = null): String

    companion object {
        val builtin = mapOf(
            "context" to ContextEvaluator,
            "datetime" to DateTimeEvaluator,
        )
    }
}
