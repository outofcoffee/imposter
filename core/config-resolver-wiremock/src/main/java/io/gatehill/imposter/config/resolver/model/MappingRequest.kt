package io.gatehill.imposter.config.resolver.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class MappingRequest(
    val method: String?,
    val url: String?,
    val headers: Map<String, Map<String, String>>?,
    val bodyPatterns: List<BodyPattern>?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class BodyPattern(
    @JsonProperty("matchesXPath")
    val matchesXPath: Any?,

    @JsonProperty("xPathNamespaces")
    val xPathNamespaces: Map<String, String>?,
)
