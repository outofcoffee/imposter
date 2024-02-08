/*
 * Copyright (c) 2023-2024.
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

package io.gatehill.imposter.expression.eval

import io.gatehill.imposter.util.splitOnCommaAndTrim
import org.apache.logging.log4j.LogManager
import java.util.UUID

object RandomEvaluator : ExpressionEvaluator<String> {
    override val name = "random"

    val alphabetUpper = ('A'..'Z')
    val alphabetLower = ('a'..'z')
    val numbers = ('0'..'9')

    private val LOGGER = LogManager.getLogger(RandomEvaluator::class.java)

    override fun eval(expression: String, context: Map<String, *>): String? {
        try {
            val parts = expression.split(
                delimiters = arrayOf("."),
                ignoreCase = false,
                limit = 2,
            )
            if (parts.size < 2) {
                LOGGER.warn("Could not parse random expression: $expression")
                return ""
            }
            return parse(parts[1])
        } catch (e: Exception) {
            throw RuntimeException("Error replacing placeholder '$expression' with random value", e)
        }
    }

    private fun parse(randomConfig: String): String? {
        val parenIdx = randomConfig.indexOf('(')
        val rawArgs = randomConfig.substring(parenIdx + 1, randomConfig.length - 1)
        val args = rawArgs.splitOnCommaAndTrim().filter { it.isNotBlank() }.associate {
            it.split("=").let { parts -> parts[0] to parts[1] }
        }
        val type = randomConfig.substringBefore("(")
        val length = args["length"]?.toInt() ?: 1
        val uppercase = args["uppercase"].toBoolean()

        // the chars string must be enclosed in double quotes
        val chars = args["chars"]?.let {
            if (!it.startsWith("\"") || !it.endsWith("\"")) {
                LOGGER.warn("chars string must be enclosed in double quotes")
                return null
            }
            it.substring(1, it.length - 1)
        }

        val random = when (type) {
            "alphabetic" -> getRandomString(length, alphabetUpper + alphabetLower)
            "alphanumeric" -> getRandomString(length, alphabetUpper + alphabetLower + numbers)
            "any" -> {
                if (chars == null) {
                    LOGGER.warn("chars string must be provided for random type 'any'")
                    return null
                }
                getRandomString(length, chars.toList())
            }
            "numeric" -> getRandomString(length, numbers.toList())
            "uuid" -> UUID.randomUUID().toString()
            else -> {
                LOGGER.warn("Could not parse random expression: $randomConfig")
                return null
            }
        }
        return if (uppercase) random.uppercase() else random
    }

    private fun getRandomString(length: Int, allowedChars: List<Char>) = (1..length)
            .map { allowedChars.random() }
            .joinToString("")
}
