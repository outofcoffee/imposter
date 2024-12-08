/*
 * Copyright (c) 2024.
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

package io.gatehill.imposter.scripting.graalvm.proxy

import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import org.graalvm.polyglot.proxy.ProxyObject

/**
 * Proxies a POJO object. All retrieved elements are proxied.
 */
class PojoProxyObject(private val obj: Any) : ProxyObject {
    companion object {
        val excludedMembers = setOf("class")
    }

    private val memberKeys by lazy<List<String>> {
        val fieldNames = obj::class.java.fields.map { it.name }
        val accessors = obj::class.java.methods.filter { it.name.startsWith("get") || it.name.startsWith("is") }.map {
            val memberName = it.name.removePrefix("get").removePrefix("is")
            memberName.substring(0, 1).lowercase() + memberName.substring(1)
        }
        (fieldNames + accessors - excludedMembers).distinct()
    }

    override fun getMember(key: String): Any? {
        if (!memberKeys.contains(key)) {
            return null
        }

        val getterName = toMemberForm(key, "get")
        val boolAccessorName = toMemberForm(key, "it")

        val value = if (obj::class.java.fields.any { it.name == key }) {
            obj::class.java.getField(key).let {
                it.isAccessible = true
                it.get(obj)
            }
        } else if (obj::class.java.methods.any { it.name == getterName }) {
            obj::class.java.getMethod(getterName).let {
                it.isAccessible = true
                it.invoke(obj)
            }
        } else if (obj::class.java.methods.any { it.name == boolAccessorName }) {
            obj::class.java.getMethod(boolAccessorName).let {
                it.isAccessible = true
                it.invoke(obj)
            }
        } else null

        return value?.let(DeepProxy::of)
    }

    override fun getMemberKeys(): Any = ProxyArray.fromList(memberKeys)

    override fun hasMember(key: String): Boolean = memberKeys.contains(key)

    override fun putMember(key: String, value: Value?) {
        val v = value?.let { if (value.isHostObject) value.asHostObject() else value }
        val setterName = toMemberForm(key, "set")

        val field = obj::class.java.fields.find { it.name == key }
        if (field != null) {
            field.isAccessible = true
            field.set(obj, v)
        } else {
            val setters = obj::class.java.methods.filter { it.name == setterName && it.parameterCount == 1 }
            if (setters.size != 1) {
                throw UnsupportedOperationException("No field or single setter found for member: $key")
            }
            val setter = setters.first()
            setter.isAccessible = true
            setter.invoke(obj, v)
        }
    }

    private fun toMemberForm(name: String, prefix: String) =
        prefix + name.substring(0, 1).uppercase() + name.substring(1)
}
