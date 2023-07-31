package io.gatehill.imposter.service

import io.gatehill.imposter.http.HttpExchange
import io.gatehill.imposter.plugin.config.capture.ItemCaptureConfig

interface CaptureService {
    fun captureItem(
            captureConfigKey: String,
            itemConfig: ItemCaptureConfig,
            httpExchange: HttpExchange
    )
}
