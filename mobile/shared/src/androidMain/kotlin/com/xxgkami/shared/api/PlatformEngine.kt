package com.xxgkami.shared.api

import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import okhttp3.CertificatePinner
import java.util.concurrent.TimeUnit

/**
 * Android 平台 HTTP 引擎工厂。
 *
 * 配置 OkHttp 引擎，可选启用 SSL 证书锁定（Certificate Pinning），
 * 防止中间人攻击（MITM）即使设备安装了恶意 CA 证书。
 *
 * ## 证书锁定启用条件
 * 仅当 [CERT_PIN_SHA256] 被设置为真实证书指纹（非占位符）时才启用证书锁定。
 * 默认情况下（占位符或空字符串）禁用证书锁定，避免 HTTPS 请求全部失败。
 *
 * ## 如何生成证书 SHA-256 指纹
 *
 * 方式一（推荐，使用 OpenSSL）：
 * ```
 * echo | openssl s_client -connect your-api-domain.com:443 2>/dev/null \
 *   | openssl x509 -pubkey -noout \
 *   | openssl pkey -pubin -outform der \
 *   | openssl dgst -sha256 -binary \
 *   | openssl base64
 * ```
 * 输出形如：`sha256/abc123...=`
 *
 * 方式二（使用 keytool，针对 JKS/PKCS12 证书文件）：
 * ```
 * keytool -printcert -rfc -file your-cert.pem \
 *   | openssl x509 -pubkey -noout \
 *   | openssl pkey -pubin -outform der \
 *   | openssl dgst -sha256 -binary \
 *   | openssl base64
 * ```
 *
 * 生成后将 base64 字符串填入 [CERT_PIN_SHA256]（不含 `sha256/` 前缀）。
 *
 * ## 注意事项
 * - 证书锁定会 pin 住特定证书或公钥，证书轮换时需同步更新此值并发布新版本
 * - 建议同时 pin 主证书和备份证书，避免证书更新导致应用无法联网
 * - 调试构建可暂时留空 [CERT_PIN_SHA256] 以使用系统默认信任链
 */
actual fun createPlatformEngine(host: String): HttpClientEngine? {
    val engineConfig: OkHttpConfig.() -> Unit = {
        // 仅当配置了真实证书指纹（非占位符且非空）时启用证书锁定
        if (CERT_PIN_SHA256.isNotBlank() && CERT_PIN_SHA256 != PLACEHOLDER_PIN) {
            val certificatePinner = CertificatePinner.Builder()
                .add(host, "sha256/$CERT_PIN_SHA256")
                .build()
            certificatePinner(certificatePinner)
        }
        connectTimeout(10, TimeUnit.SECONDS)
        readTimeout(15, TimeUnit.SECONDS)
        writeTimeout(15, TimeUnit.SECONDS)
    }

    return OkHttpEngine.create(engineConfig)
}

/**
 * 服务器证书的 SHA-256 公钥指纹（Base64 编码，不含 `sha256/` 前缀）。
 *
 * - 默认值为 [PLACEHOLDER_PIN] 占位符，此时禁用证书锁定
 * - 生产环境部署前必须替换为真实证书指纹
 * - 留空字符串同样会禁用证书锁定
 */
private const val CERT_PIN_SHA256 = "REPLACE_WITH_YOUR_CERTIFICATE_SHA256_PIN"

/** 证书指纹占位符，用于检测是否已配置真实指纹 */
private const val PLACEHOLDER_PIN = "REPLACE_WITH_YOUR_CERTIFICATE_SHA256_PIN"
