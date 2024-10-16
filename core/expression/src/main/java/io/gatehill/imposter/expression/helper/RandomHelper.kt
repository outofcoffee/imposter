/*
 * Copyright (c) 2024-2024.
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

package io.gatehill.imposter.expression.helper

import org.apache.logging.log4j.LogManager
import java.util.*

/**
 * Generates random values in different formats.
 */
object RandomHelper {
    private val logger = LogManager.getLogger(RandomHelper::class.java)

    internal val alphabetUpper = ('A'..'Z')
    internal val alphabetLower = ('a'..'z')
    internal val numbers = ('0'..'9')

    fun alphabetic(length: Int, uppercase: Boolean): String {
        return applyCase(uppercase, getRandomString(length, alphabetUpper + alphabetLower))
    }

    fun alphanumeric(length: Int, uppercase: Boolean): String {
        return applyCase(uppercase, getRandomString(length, alphabetUpper + alphabetLower + numbers))
    }

    fun any(length: Int, uppercase: Boolean, chars: String?): String? {
        if (chars == null) {
            logger.warn("chars string must be provided for random type 'any'")
            return null
        }
        return applyCase(uppercase, getRandomString(length, chars.toList()))
    }

    fun numeric(length: Int, uppercase: Boolean): String {
        return applyCase(uppercase, getRandomString(length, numbers.toList()))
    }

    fun uuid(uppercase: Boolean): String {
        return applyCase(uppercase, UUID.randomUUID().toString())
    }

    private fun applyCase(uppercase: Boolean, value: String): String =
        if (uppercase) value.uppercase() else value

    private fun getRandomString(length: Int, allowedChars: List<Char>) = (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}
