package com.xxgkami.shared.api

import io.ktor.client.engine.*

/**
 * 平台相关 HTTP 引擎工厂 expect 声明。
 * 各平台提供 actual 实现：
 * - Android: 返回带证书锁定的 OkHttp 引擎
 * - iOS/Desktop: 返回 null（使用平台默认引擎，不做证书锁定）
 */
expect fun createPlatformEngine(host: String): HttpClientEngine?
