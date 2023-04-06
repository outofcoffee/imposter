package io.gatehill.imposter.script.dsl

import io.gatehill.imposter.util.InjectorUtil
import io.gatehill.imposter.util.supervisedIOCoroutineScope
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.awaitResult
import kotlinx.coroutines.runBlocking

class ScriptHttpRequestBuilderImpl : ScriptHttpRequestBuilder {
    private var url: String? = null
    private var method: String? = null
    private var headers: Map<String, String>? = null
    private var body: String? = null

    override fun url(url: String): ScriptHttpRequestBuilder {
        this.url = url
        return this
    }

    override fun method(method: String): ScriptHttpRequestBuilder {
        this.method = method
        return this
    }

    override fun header(name: String, value: String): ScriptHttpRequestBuilder {
        if (null == headers) {
            headers = mutableMapOf()
        }
        (headers as MutableMap)[name] = value
        return this
    }

    override fun body(body: String): ScriptHttpRequestBuilder {
        this.body = body
        return this
    }

    override fun execute(): ScriptHttpResponse {
        val httpClient = InjectorUtil.getInstance<Vertx>().createHttpClient()
        return runBlocking(supervisedIOCoroutineScope.coroutineContext) {
            val request = httpClient.request(HttpMethod(method), url).await()

            if (!body.isNullOrBlank()) {
                request.write(Buffer.buffer(body))
            }
            if (null != headers) {
                headers!!.forEach { request.putHeader(it.key, it.value) }
            }
            val resp = awaitResult { h -> request.send(h) }
            val response = ScriptHttpResponse(
                statusCode = resp.statusCode(),
                headers = resp.headers().associate { it.key to it.value },
                body = resp.body().result().toString(),
            )
            return@runBlocking response
        }
    }
}
