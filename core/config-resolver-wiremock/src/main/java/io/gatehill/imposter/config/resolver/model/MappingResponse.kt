package io.gatehill.imposter.config.resolver.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MappingResponse(
    val status: Int?,
    val bodyFileName: String?,
    val headers: Map<String, String>?,
    val transformers: List<String>?,
)
