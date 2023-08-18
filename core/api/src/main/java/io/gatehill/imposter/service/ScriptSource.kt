package io.gatehill.imposter.service

data class ScriptSource(
    val code: String?,
    val file: String?,
) {
    val valid get() = null != code || null != file
}
