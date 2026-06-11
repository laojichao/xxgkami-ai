package com.xxgkami.shared.api

import io.ktor.client.engine.*

/**
 * Desktop 平台：使用 CIO 引擎默认配置，不额外做证书锁定。
 * Desktop 环境通常由操作系统级别的证书存储提供安全保障。
 */
actual fun createPlatformEngine(host: String): HttpClientEngine? = null
