package io.gatehill.imposter.config.resolver.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class WiremockMapping(
    val request: MappingRequest,
    val response: MappingResponse,
)
