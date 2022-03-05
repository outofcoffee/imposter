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
     * If no expression is found, [expression] is returned.
     */
    fun eval(expression: String, httpExchange: HttpExchange): String

    /**
     * Loads a value for the specified key, optionally applying a JsonPath query
     * to the value.
     *
     * The [rawItemKey] can be in the form of a string such as `a.b.c`, or, optionally
     * include a JsonPath query, prefixed with a colon, such as `a.b.c:$.jp`, where
     * `$.jp` is a valid JsonPath expression.
     *
     * @param rawItemKey the placeholder key
     * @param valueResolver the function to resolve the value, prior to any querying
     */
    fun <T:Any> loadAndQuery(rawItemKey: String, valueResolver: (key: String) -> T?): T?
}
