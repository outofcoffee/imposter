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

package io.gatehill.imposter.scripting.graalvm.storeproxy

import io.gatehill.imposter.http.ExchangePhase
import io.gatehill.imposter.store.core.Store
import io.gatehill.imposter.util.MapUtil
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyObject

class ObjectProxyingStore(private val delegate: Store) : Store {
    override val storeName: String
        get() = delegate.storeName
    override val typeDescription: String
        get() = delegate.typeDescription
    override val isEphemeral: Boolean
        get() = delegate.isEphemeral

    override fun save(key: String, value: Any?, phase: ExchangePhase) = delegate.save(key, value)

    override fun <T> load(key: String): T? {
        val value = delegate.load<T?>(key)

        @Suppress("UNCHECKED_CAST")
        return value?.let(DeepProxy::of) as T?
    }

    override fun loadAsJson(key: String): String {
        // don't use the intercepted load() function
        return MapUtil.jsonify(delegate.load(key))
    }

    override fun delete(key: String) = delegate.delete(key)

    override fun loadAll() = delegate.loadAll()

    override fun loadByKeyPrefix(keyPrefix: String) = delegate.loadByKeyPrefix(keyPrefix)

    override fun hasItemWithKey(key: String) = delegate.hasItemWithKey(key)

    override fun count() = delegate.count()
}

object DeepProxy {
    fun of(obj: Any): Any {
        return when (obj) {
            is Array<*> -> InterceptingList(obj.toList())
            is List<*> -> InterceptingList(obj)
            is Map<*, *> -> InterceptingObject(obj)
            else -> obj
        }
    }
}

class InterceptingList(private val src: List<Any?>) : ProxyArray {
    override fun get(index: Long): Any? {
        if (index < 0 || index >= src.size) {
            throw IndexOutOfBoundsException("Index out of bounds: $index, size: ${src.size}")
        }
        val value = src[index.toInt()]
        return value?.let(DeepProxy::of)
    }

    override fun set(index: Long, value: Value?) {
        check(src is MutableList)
        value?.also {
            val valueToSet = if (value.isHostObject) value.asHostObject() else value
            src[index.toInt()] = valueToSet
        }
    }

    override fun getSize(): Long {
        return src.size.toLong()
    }
}

class InterceptingObject(private val src: Map<*, *>) : ProxyObject {
    override fun getMember(key: String?): Any? {
        val value = src[key]
        return value?.let(DeepProxy::of)
    }

    override fun getMemberKeys(): Any {
        return ProxyArray.fromList(src.keys.toList())
    }

    override fun hasMember(key: String?): Boolean {
        return key?.let { src.containsKey(key) } ?: false
    }

    override fun putMember(key: String, value: Value?) {
        check(src is MutableMap)
        value?.also {
            @Suppress("UNCHECKED_CAST")
            (src as MutableMap<Any?, Any?>)[key] = if (value.isHostObject) value.asHostObject() else value
        }
    }

    override fun removeMember(key: String?): Boolean {
        check(src is MutableMap)
        if (src.containsKey(key)) {
            @Suppress("UNCHECKED_CAST")
            (src as MutableMap<Any?, Any?>).remove<Any?, Any?>(key)
            return true
        } else {
            return false
        }
    }
}
