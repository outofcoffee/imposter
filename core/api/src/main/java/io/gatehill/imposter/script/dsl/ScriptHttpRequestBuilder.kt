package io.gatehill.imposter.script.dsl

interface ScriptHttpRequestBuilder {
    fun url(url: String): ScriptHttpRequestBuilder
    fun method(method: String): ScriptHttpRequestBuilder
    fun header(name: String, value: String): ScriptHttpRequestBuilder
    fun body(body: String): ScriptHttpRequestBuilder
    fun execute(): ScriptHttpResponse
}

data class ScriptHttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String,
)
