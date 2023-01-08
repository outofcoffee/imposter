package io.gatehill.imposter.expression.eval

import org.apache.logging.log4j.LogManager
import java.util.*

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
        val args = rawArgs.split(",").map { it.trim() }.filter { it.isNotBlank() }.associate {
            it.split("=").let { parts -> parts[0] to parts[1] }
        }
        val type = randomConfig.substringBefore("(")
        val length = args["length"]?.toInt()
        val uppercase = args["uppercase"].toBoolean()

        val random = when (type) {
            "alphabetic" -> getRandomString(length!!, alphabetUpper + alphabetLower)
            "alphanumeric" -> getRandomString(length!!, alphabetUpper + alphabetLower + numbers)
            "numeric" -> getRandomString(length!!, numbers.toList())
            "uuid" -> UUID.randomUUID().toString()
            else -> run {
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
