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

package io.gatehill.imposter.script

import io.gatehill.imposter.util.CollectionUtil

/**
 * Lowercases keys on instantiation, on putting an entry, before getting an entry,
 * removing an entry or checking for a given key.
 */
class LowercaseKeysMap<V>(entries: Map<String, V>) : HashMap<String, V>(
    CollectionUtil.convertKeysToLowerCase(entries)
) {
    override fun put(key: String, value: V): V? {
        return super.put(key.lowercase(), value)
    }

    override fun putIfAbsent(key: String, value: V): V? {
        return super.putIfAbsent(key.lowercase(), value)
    }

    override fun putAll(from: Map<out String, V>) {
        super.putAll(from.mapKeys { it.key.lowercase() })
    }

    override fun get(key: String): V? {
        return super.get(key.lowercase())
    }

    override fun getOrDefault(key: String, defaultValue: V): V {
        return super.getOrDefault(key.lowercase(), defaultValue)
    }

    override fun containsKey(key: String): Boolean {
        return super.containsKey(key.lowercase())
    }

    override fun remove(key: String): V? {
        return super.remove(key.lowercase())
    }

    override fun remove(key: String, value: V): Boolean {
        return super.remove(key.lowercase(), value)
    }

    override fun replace(key: String, value: V): V? {
        return super.replace(key.lowercase(), value)
    }

    override fun replace(key: String, oldValue: V, newValue: V): Boolean {
        return super.replace(key.lowercase(), oldValue, newValue)
    }
}
