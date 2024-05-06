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

package io.gatehill.imposter.scripting.graalvm

import io.gatehill.imposter.script.ExecutionContext
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyObject

/**
 * Graal polyglot object proxy for [ExecutionContext.Request].
 */
class RequestProxy(
    private val request: ExecutionContext.Request
) : ProxyObject {
    private val properties = arrayOf(
        "path",
        "method",
        "uri",
        "headers",
        "pathParams",
        "queryParams",
        "formParams",
        "body",
        "normalisedHeaders",
    )

    override fun getMember(key: String?): Any? = when (key) {
        "path" -> request.path
        "method" -> request.method
        "uri" -> request.uri
        "headers" -> MapObjectProxy(request.headers)
        "pathParams" -> MapObjectProxy(request.pathParams)
        "queryParams" -> MapObjectProxy(request.queryParams)
        "formParams" -> MapObjectProxy(request.formParams)
        "body" -> request.body
        "normalisedHeaders" -> MapObjectProxy(request.normalisedHeaders)
        else -> null
    }

    override fun getMemberKeys(): Array<*> = properties

    override fun hasMember(key: String?) =
        key?.let { properties.contains(key) } ?: false

    override fun putMember(key: String?, value: Value?) {
        throw UnsupportedOperationException("Request cannot be modified")
    }
}

class MapObjectProxy(private val orig: Map<String, *>): ProxyObject {
    override fun getMember(key: String?): Any? {
        return key?.let { orig[key] }
    }

    override fun getMemberKeys(): Any {
        return orig.keys.toTypedArray()
    }

    override fun hasMember(key: String?): Boolean {
        return key?.let {  orig.containsKey(key) } ?: false
    }

    override fun putMember(key: String?, value: Value?) {
        throw UnsupportedOperationException("Object is unmodifiable")
    }
}
