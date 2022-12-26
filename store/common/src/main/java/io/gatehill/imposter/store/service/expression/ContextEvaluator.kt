/*
 * Copyright (c) 2022.
 *
 * This file is part of Imposter.
 *
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as
 * defined below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights
 * under the License will not include, and the License does not grant to
 * you, the right to Sell the Software.
 *
 * For purposes of the foregoing, "Sell" means practicing any or all of
 * the rights granted to you under the License to provide to third parties,
 * for a fee or other consideration (including without limitation fees for
 * hosting or consulting/support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from
 * the functionality of the Software. Any license notice or attribution
 * required by the License must also include this Commons Clause License
 * Condition notice.
 *
 * Software: Imposter
 *
 * License: GNU Lesser General Public License version 3
 *
 * Licensor: Peter Cornish
 *                                                                                   
 * Imposter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *                                                                                   
 * Imposter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *                                                                                   
 * You should have received a copy of the GNU Lesser General Public License
 * along with Imposter.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.gatehill.imposter.store.service.expression

import io.gatehill.imposter.http.ExchangePhase
import org.apache.logging.log4j.LogManager

/**
 * Evaluates a context expression in the form:
 * ```
 * context.a.b.c
 * ```
 */
object ContextEvaluator : HttpExpressionEvaluator<String>() {
    private val LOGGER = LogManager.getLogger(ContextEvaluator::class.java)

    override val name = "context"

    override fun eval(expression: String, context: Map<String, *>): String? {
        try {
            val parts = expression.split(
                delimiters = arrayOf("."),
                ignoreCase = false,
                limit = 4,
            )
            val httpExchange = getHttpExchange(context)
            val parsed: String? = when (parts[0]) {
                "context" -> when (parts[1]) {
                    "request" -> {
                        when (parts[2]) {
                            "body" -> checkExpression(expression, 3, parts) {
                                httpExchange.request().bodyAsString
                            }
                            "headers" -> checkExpression(expression, 4, parts) {
                                httpExchange.request().getHeader(parts[3])
                            }
                            "pathParams" -> checkExpression(expression, 4, parts) {
                                httpExchange.request().pathParam(parts[3])
                            }
                            "queryParams" -> checkExpression(expression, 4, parts) {
                                httpExchange.request().queryParam(parts[3])
                            }
                            else -> {
                                LOGGER.warn("Could not parse request context expression: $expression")
                                null
                            }
                        }
                    }
                    "response" -> {
                        check(httpExchange.phase == ExchangePhase.RESPONSE_SENT) {
                            "Cannot capture response outside of ${ExchangePhase.RESPONSE_SENT} phase"
                        }
                        when (parts[2]) {
                            "body" -> checkExpression(expression, 3, parts) {
                                httpExchange.response().bodyBuffer?.toString(Charsets.UTF_8)
                            }
                            "headers" -> checkExpression(expression, 4, parts) {
                                httpExchange.response().headers()[parts[3]]
                            }
                            else -> {
                                LOGGER.warn("Could not parse response context expression: $expression")
                                null
                            }
                        }
                    }
                    else -> {
                        LOGGER.warn("Could not parse context expression: $expression")
                        null
                    }
                }
                else -> {
                    LOGGER.warn("Could not parse context expression: $expression")
                    null
                }
            }
            return parsed

        } catch (e: Exception) {
            throw RuntimeException("Error evaluating context expression: $expression", e)
        }
    }

    private fun checkExpression(
        expression: String,
        minParts: Int,
        parts: List<String>,
        valueSupplier: () -> String?,
    ): String? {
        if (parts.size < minParts) {
            LOGGER.warn("Could not parse context expression: $expression - expected $minParts parts, but was ${parts.size}")
            return null
        }
        return valueSupplier()
    }
}
