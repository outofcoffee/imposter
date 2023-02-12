/*
 * Copyright (c) 2022-2023.
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

package io.gatehill.imposter.placeholder

import io.gatehill.imposter.expression.eval.ExpressionEvaluator
import io.gatehill.imposter.util.DateTimeUtil
import org.apache.logging.log4j.LogManager
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * Evaluates a datetime expression in the form:
 * ```
 * datetime.a.b
 * ```
 */
object DateTimeEvaluator : ExpressionEvaluator<String> {
    private val LOGGER = LogManager.getLogger(DateTimeEvaluator::class.java)

    override val name = "datetime"

    override fun eval(expression: String, context: Map<String, *>): String? {
        try {
            val parts = expression.split(
                delimiters = arrayOf("."),
                ignoreCase = false,
                limit = 3,
            )
            if (parts.size < 3) {
                LOGGER.warn("Could not parse datetime expression: $expression")
                return null
            }
            val parsed = when (parts[0]) {
                "datetime" -> when (parts[1]) {
                    "now" -> when (parts[2]) {
                        "millis" -> System.currentTimeMillis().toString()
                        "nanos" -> System.nanoTime().toString()
                        "iso8601_date" -> DateTimeUtil.DATE_FORMATTER.format(
                            LocalDateTime.now().truncatedTo(ChronoUnit.DAYS)
                        )
                        "iso8601_datetime" -> DateTimeUtil.DATE_TIME_FORMATTER.format(
                            OffsetDateTime.now().truncatedTo(ChronoUnit.MILLIS)
                        )
                        else -> {
                            LOGGER.warn("Could not parse datetime expression: $expression")
                            null
                        }
                    }
                    else -> {
                        LOGGER.warn("Could not parse datetime expression: $expression")
                        null
                    }
                }
                else -> {
                    LOGGER.warn("Could not parse datetime expression: $expression")
                    null
                }
            }
            return parsed

        } catch (e: Exception) {
            throw RuntimeException("Error evaluating datetime expression: $expression", e)
        }
    }
}