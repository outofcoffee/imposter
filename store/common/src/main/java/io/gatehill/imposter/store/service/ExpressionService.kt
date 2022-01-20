package io.gatehill.imposter.store.service

import io.gatehill.imposter.http.HttpExchange

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
     */
    fun eval(expression: String, httpExchange: HttpExchange): String?
}
