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

package io.gatehill.imposter.config.loader.kryo

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.apache.logging.log4j.LogManager
import java.io.File

object BinarySerialiser {
    private val logger = LogManager.getLogger(BinarySerialiser::class.java)

    private val kryo = Kryo().apply {
        isRegistrationRequired = false
        references = true
    }

    fun buildKryoPath(file: File): String {
        return file.absolutePath + ".bin"
    }

    fun serialise(obj: Any, filePath: String) {
        File(filePath).outputStream().use {
            val output = Output(it)
            kryo.writeClassAndObject(output, obj)
            output.close()
        }
        logger.debug("Serialised to $filePath")
    }

    fun <T> deserialise(filePath: String): T {
        logger.debug("Deserialising from $filePath")
        return File(filePath).inputStream().use {
            val input = Input(it)
            @Suppress("UNCHECKED_CAST")
            val obj = kryo.readClassAndObject(input) as T
            input.close()
            obj
        }
    }
}
