package com.xxgkami.shared.api

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import okhttp3.CertificatePinner
import java.util.concurrent.TimeUnit

/**
 * Android 平台 HTTP 引擎工厂。
 *
 * 配置 OkHttp 引擎并启用 SSL 证书锁定（Certificate Pinning），
 * 防止中间人攻击（MITM）即使设备安装了恶意 CA 证书。
 *
 * TODO: 将 [CERT_PIN_SHA256] 替换为你服务器证书的实际 SHA-256 指纹。
 * 获取方式：
 *   openssl s_client -connect your-api-domain.com:443 | openssl x509 -pubkey -noout \
 *     | openssl rsa -pubin -outform der | openssl dgst -sha256 -binary | base64
 */
actual fun createPlatformEngine(host: String): HttpClientEngine {
    val certificatePinner = CertificatePinner.Builder()
        .add(host, "sha256/$CERT_PIN_SHA256")
        .build()

    return OkHttpEngine.create {
        config {
            certificatePinner(certificatePinner)
            connectTimeout(10, TimeUnit.SECONDS)
            readTimeout(15, TimeUnit.SECONDS)
            writeTimeout(15, TimeUnit.SECONDS)
        }
    }
}

// TODO: 替换为你服务器证书的 SHA-256 指纹
private const val CERT_PIN_SHA256 = "REPLACE_WITH_YOUR_CERTIFICATE_SHA256_PIN"
