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

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.store.factory.StoreFactory
import org.apache.logging.log4j.LogManager

/**
 * Evaluates a store expression in the form:
 * ```
 * storename.itemkey
 * ```
 */
class StoreEvaluator(
    private val storeFactory: StoreFactory,
) : ExpressionEvaluator<Any> {
    override fun eval(expression: String, httpExchange: HttpExchange): Any? {
        try {
            val parts = expression.split(
                delimiters = arrayOf("."),
                ignoreCase = false,
                limit = 2,
            )
            if (parts.size < 2) {
                LOGGER.warn("Could not parse store expression: $expression")
                return ""
            }
            val storeName = parts[0]
            val itemKey = parts[1]

            val store = storeFactory.getStoreByName(storeName, false)
            val itemValue = store.load<Any>(itemKey)
            LOGGER.trace("Loaded value for key: {} in store: {} as: {}", itemKey, storeName, itemValue)
            return itemValue

        } catch (e: Exception) {
            throw RuntimeException("Error replacing template placeholder '$expression' with store item", e)
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(StoreEvaluator::class.java)
    }
}
