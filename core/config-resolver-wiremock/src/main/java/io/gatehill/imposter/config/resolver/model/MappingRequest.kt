package io.gatehill.imposter.config.resolver.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MappingRequest(
    val method: String?,
    val url: String?,
    val headers: Map<String, Map<String, String>>?,
    val bodyPatterns: List<Map<String, *>>?,
)
