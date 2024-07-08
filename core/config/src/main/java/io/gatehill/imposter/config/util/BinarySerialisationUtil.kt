package io.gatehill.imposter.config.util

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.nustaq.serialization.FSTConfiguration
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object BinarySerialisationUtil {
    private val kryo = Kryo().apply {
        isRegistrationRequired = false
        references = true
    }

    private val conf: FSTConfiguration by lazy { FSTConfiguration.createDefaultConfiguration() }

    fun serialise(obj: Any, filePath: String) = serialiseKryo(obj, filePath)
    fun <T> deserialise(filePath: String) = deserialiseKryo<T>(filePath)

    /**
     * Serialises an object to a file using FST.
     */
    fun serialiseFst(obj: Any, filePath: String) {
        File(filePath).outputStream().use {
            val output = conf.asByteArray(obj)
            it.write(output)
        }
    }

    /**
     * Deserialises an object from a file using FST.
     */
    fun <T> deserialiseFst(filePath: String): T {
        return File(filePath).inputStream().use {
            conf.asObject(it.readAllBytes()) as T
        }
    }

    /**
     * Serialises an object to a file using Kryo.
     */
    fun serialiseKryo(obj: Any, filePath: String) {
        File(filePath).outputStream().use {
            val output = Output(it)
            kryo.writeClassAndObject(output, obj)
            output.close()
        }
    }

    /**
     * Deserialises an object from a file using Kryo.
     */
    fun <T> deserialiseKryo(filePath: String): T {
        return File(filePath).inputStream().use {
            val input = Input(it)
            val obj = kryo.readClassAndObject(input) as T
            input.close()
            obj
        }
    }

    fun serialiseJOS(obj: Any, filePath: String) {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            ObjectOutputStream(byteArrayOutputStream).use { objectOutputStream ->
                objectOutputStream.writeObject(obj)
                java.io.File(filePath).writeBytes(byteArrayOutputStream.toByteArray())
            }
        }
    }

    fun <T> deserialiseJOS(filePath: String): T {
        val bytes = java.io.File(filePath).readBytes()
        return ByteArrayInputStream(bytes).use { byteArrayInputStream ->
            ObjectInputStream(byteArrayInputStream).use { objectInputStream ->
                objectInputStream.readObject()
            }
        } as T
    }
}
