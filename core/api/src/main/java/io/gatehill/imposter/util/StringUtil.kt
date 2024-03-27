package io.gatehill.imposter.util

/**
 * Splits a string on commas and trims each element.
 * If the input is `null`, returns an empty list.
 */
fun String?.splitOnCommaAndTrim(): List<String> =
    this?.split(",")?.map { it.trim() } ?: emptyList()
