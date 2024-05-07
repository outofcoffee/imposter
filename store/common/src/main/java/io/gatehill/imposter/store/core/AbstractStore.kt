/*
 * Copyright (c) 2016-2021.
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
package io.gatehill.imposter.store.core

import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.service.DeferredOperationService
import io.gatehill.imposter.util.MapUtil
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * @author Pete Cornish
 */
abstract class AbstractStore(
    private val deferredOperationService: DeferredOperationService,
) : Store {
    private val logger: Logger = LogManager.getLogger(AbstractStore::class.java)

    override fun loadAsJson(key: String): String {
        return MapUtil.jsonify(load(key))
    }

    override fun save(key: String, value: Any?, phase: ExchangePhase) {
        if (phase == ExchangePhase.RESPONSE_SENT && isEphemeral) {
            throw IllegalStateException("Cannot use deferred persistence for ephemeral store: $storeName of type: $typeDescription")
        }
        when (phase) {
            ExchangePhase.REQUEST_RECEIVED -> save(key, value)
            ExchangePhase.RESPONSE_SENT -> deferSave(key, value)
            else -> throw IllegalStateException("Unsupported exchange phase for store persistence: $phase")
        }
    }

    private fun deferSave(key: String, value: Any?) {
        logger.debug("Deferring persistence of item: $key to store: $storeName")
        deferredOperationService.defer("Write item: $key to store: $storeName") {
            save(key, value)
        }
    }

    abstract fun save(key: String, value: Any?)
}
