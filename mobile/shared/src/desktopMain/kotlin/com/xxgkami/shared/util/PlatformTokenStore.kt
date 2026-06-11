package com.xxgkami.shared.util

import com.xxgkami.shared.api.ApiProvider
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Desktop平台Token安全持久化 actual实现
 *
 * 使用 AES-256-GCM 加密文件存储 Token，具备以下安全特性：
 * - AES-256-GCM 认证加密，防止密文被篡改
 * - 每次加密使用随机 IV（初始化向量），避免相同明文产生相同密文
 * - 密钥文件独立存储在用户主目录下（.xxgkami/.key）
 * - Token 文件和密钥文件权限隔离
 *
 * 存储位置：~/.xxgkami/tokens.enc
 * 密钥位置：~/.xxgkami/.key
 *
 * 文件格式：Base64( IV[12 bytes] + AES-GCM-Ciphertext + AuthTag[16 bytes] )
 */
@Serializable
private data class TokenPair(val access: String, val refresh: String)

actual class PlatformTokenStore actual constructor() {
    /** 配置目录：用户主目录下的 .xxgkami 隐藏文件夹 */
    private val configDir = File(System.getProperty("user.home"), ".xxgkami")

    /** 加密后的 Token 文件 */
    private val tokenFile = File(configDir, "tokens.enc")

    /** AES-256 密钥文件（Base64 编码） */
    private val keyFile = File(configDir, ".key")

    /** 随机数生成器，用于生成密钥和 IV */
    private val secureRandom = SecureRandom()

    init {
        // 确保配置目录存在
        if (!configDir.exists() && !configDir.mkdirs()) {
            throw IllegalStateException("Failed to create config directory: ${configDir.absolutePath}")
        }
    }

    actual fun saveTokens(accessToken: String, refreshToken: String) {
        val data = Json.encodeToString(TokenPair(accessToken, refreshToken))
        val encrypted = encrypt(data)
        tokenFile.writeText(encrypted)
        ApiProvider.setTokens(accessToken, refreshToken)
    }

    actual fun getAccessToken(): String? {
        val tokens = loadTokens()
        return tokens?.first
    }

    actual fun getRefreshToken(): String? {
        val tokens = loadTokens()
        return tokens?.second
    }

    actual fun clearTokens() {
        tokenFile.delete()
        ApiProvider.clearTokens()
    }

    actual fun isLoggedIn(): Boolean = getAccessToken() != null

    /**
     * 从加密文件中加载并解密 Token 对
     *
     * @return Pair(accessToken, refreshToken)，文件不存在或解密失败返回 null
     */
    private fun loadTokens(): Pair<String, String>? {
        if (!tokenFile.exists()) return null
        return try {
            val decrypted = decrypt(tokenFile.readText())
            val tokens = try {
                Json.decodeFromString<TokenPair>(decrypted)
            } catch (e: Exception) {
                // Backwards compatibility with old pipe-delimited format
                val parts = decrypted.split("|", limit = 2)
                if (parts.size == 2) TokenPair(parts[0], parts[1])
                else null
            }
            if (tokens != null) Pair(tokens.access, tokens.refresh) else null
        } catch (e: Exception) {
            // 解密失败（文件损坏、密钥变更等），清除无效文件
            println("[PlatformTokenStore] Failed to decrypt tokens: ${e.message}")
            null
        }
    }

    /**
     * 获取或创建 AES-256 密钥
     *
     * 首次调用时生成随机 32 字节密钥并保存到 .key 文件（Base64 编码）。
     * 后续调用从文件读取已有密钥。
     *
     * @return 32 字节 AES 密钥
     */
    private fun getOrCreateKey(): ByteArray {
        if (keyFile.exists()) {
            return try {
                Base64.getDecoder().decode(keyFile.readText().trim())
            } catch (e: Exception) {
                // 密钥文件损坏，重新生成
                println("[PlatformTokenStore] Key file corrupted, regenerating: ${e.message}")
                generateAndSaveKey()
            }
        }
        return generateAndSaveKey()
    }

    /**
     * 生成新的 AES-256 密钥并保存到文件
     *
     * @return 32 字节 AES 密钥
     */
    private fun generateAndSaveKey(): ByteArray {
        val key = ByteArray(32)
        secureRandom.nextBytes(key)
        val encoded = Base64.getEncoder().encodeToString(key)
        val tmpFile = File(configDir, ".key.tmp")
        tmpFile.writeText(encoded)
        tmpFile.renameTo(keyFile)
        // Set permissions after rename
        try {
            val path = keyFile.toPath()
            if (File.separator != "\\") { // POSIX (Linux/Mac)
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-------"))
            } else { // Windows - restrict ACL to current user only
                try {
                    val aclView = Files.getFileAttributeView(path, java.nio.file.attribute.AclFileAttributeView::class.java)
                    if (aclView != null) {
                        val currentPrincipal = aclView.owner
                        val userEntry = java.nio.file.attribute.AclEntry.newBuilder()
                            .setType(java.nio.file.attribute.AclEntryType.ALLOW)
                            .setPrincipal(currentPrincipal)
                            .setPermissions(
                                java.nio.file.attribute.AclEntryPermission.READ_DATA,
                                java.nio.file.attribute.AclEntryPermission.WRITE_DATA,
                                java.nio.file.attribute.AclEntryPermission.READ_ATTRIBUTES,
                                java.nio.file.attribute.AclEntryPermission.READ_ACL
                            )
                            .build()
                        aclView.acl = listOf(userEntry)
                    }
                } catch (e: Exception) {
                    println("[PlatformTokenStore] Warning: Could not set Windows file permissions: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("[PlatformTokenStore] Warning: Could not set key file permissions: ${e.message}")
        }
        return key
    }

    /**
     * 使用 AES-256-GCM 加密明文
     *
     * @param plaintext 待加密的明文字符串
     * @return Base64 编码的密文（包含 IV 前缀）
     */
    private fun encrypt(plaintext: String): String {
        val key = getOrCreateKey()
        val keySpec = SecretKeySpec(key, "AES")

        // 生成 12 字节随机 IV（GCM 推荐长度）
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv) // 128 位认证标签
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // 将 IV 和密文拼接后 Base64 编码
        val combined = iv + encrypted
        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * 使用 AES-256-GCM 解密密文
     *
     * @param ciphertext Base64 编码的密文（包含 IV 前缀）
     * @return 解密后的明文字符串
     * @throws Exception 解密失败（密文被篡改、密钥不匹配等）
     */
    private fun decrypt(ciphertext: String): String {
        val key = getOrCreateKey()
        val keySpec = SecretKeySpec(key, "AES")

        val data = Base64.getDecoder().decode(ciphertext.trim())

        // 前 12 字节是 IV，剩余部分是 AES-GCM 密文 + 认证标签
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }
}
