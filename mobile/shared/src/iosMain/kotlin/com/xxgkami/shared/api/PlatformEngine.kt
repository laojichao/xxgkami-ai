package com.xxgkami.shared.api

import io.ktor.client.engine.*

/**
 * iOS 平台：使用 Darwin 引擎默认配置，不额外做证书锁定。
 * iOS 系统的 ATS (App Transport Security) 已提供足够的传输安全保障。
 */
actual fun createPlatformEngine(host: String): HttpClientEngine? = null
