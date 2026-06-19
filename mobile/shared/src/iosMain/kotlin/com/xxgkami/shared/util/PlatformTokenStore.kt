package com.xxgkami.shared.util

import com.xxgkami.shared.api.ApiProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.*
import platform.darwin.OSStatus

/**
 * iOS平台Token安全持久化 actual实现
 *
 * 使用 Apple Keychain Services 存储 Token，具备以下安全特性：
 * - 数据由 iOS 系统加密保护（硬件级加密）
 * - 仅本应用可访问（通过 Bundle ID 隔离）
 * - 仅在设备解锁时可访问（kSecAttrAccessibleWhenUnlockedThisDeviceOnly）
 *
 * Keychain 条目以 kSecClassGenericPassword 类型存储，
 * 以 service + account 作为唯一标识。
 *
 * 注意：使用 NSDictionary（toll-free bridged with CFDictionary）传递给 Security API。
 * 读取 Keychain 数据时使用 CFData API（CFDataGetBytePtr/CFDataGetLength）提取字节，
 * 避免 CFTypeRef 到 NSData 的桥接问题。
 */
actual class PlatformTokenStore actual constructor() {
    /** Keychain 服务标识符，用于区分本应用的 Keychain 条目 */
    private val service = "com.xxgkami.app"

    actual fun saveTokens(accessToken: String, refreshToken: String) {
        saveToKeychain("access_token", accessToken)
        saveToKeychain("refresh_token", refreshToken)
        ApiProvider.setTokens(accessToken, refreshToken)
    }

    actual fun getAccessToken(): String? = readFromKeychain("access_token")

    actual fun getRefreshToken(): String? = readFromKeychain("refresh_token")

    actual fun clearTokens() {
        deleteFromKeychain("access_token")
        deleteFromKeychain("refresh_token")
        ApiProvider.clearTokens()
    }

    actual fun isLoggedIn(): Boolean = getAccessToken() != null

    /**
     * 将字符串值保存到 Keychain
     *
     * @param key 条目名称（如 "access_token"）
     * @param value 要存储的字符串值
     * @throws IllegalStateException 当 Keychain 写入失败时抛出
     */
    private fun saveToKeychain(key: String, value: String) {
        // 先删除已有的同名条目，避免 SecItemAdd 返回 errSecDuplicateItem
        deleteFromKeychain(key)

        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

        val query = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to key,
            kSecValueData to data,
            kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            kSecAttrSynchronizable to false
        )

        val status: OSStatus = SecItemAdd(query as NSDictionary, null)
        if (status != errSecSuccess) {
            // Keychain 写入失败时抛出异常，避免静默失败导致数据丢失
            throw IllegalStateException("Failed to save keychain item '$key', status: $status")
        }
    }

    /**
     * 从 Keychain 读取字符串值
     *
     * SecItemCopyMatching 返回的 CFTypeRef 是 CFData（NSData 的 CF 等价物）。
     * 使用 CoreFoundation 的 CFData API 提取原始字节，再转换为 Kotlin String，
     * 避免 CFTypeRef -> NSData 的 toll-free bridging 在 Kotlin/Native 中的兼容性问题。
     *
     * @param key 条目名称（如 "access_token"）
     * @return 存储的字符串值，不存在则返回 null
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun readFromKeychain(key: String): String? {
        return try {
            memScoped {
                val resultPtr = alloc<CFTypeRefVar>()

                val query = mutableMapOf<Any?, Any?>(
                    kSecClass to kSecClassGenericPassword,
                    kSecAttrService to service,
                    kSecAttrAccount to key,
                    kSecReturnData to true,
                    kSecMatchLimit to kSecMatchLimitOne
                )

                val status: OSStatus = SecItemCopyMatching(
                    query as NSDictionary,
                    resultPtr.ptr
                )

                if (status != errSecSuccess || resultPtr.value == null) {
                    return null
                }

                val cfData = resultPtr.value!!
                // 使用 try-finally 确保 CFRelease 一定被调用，避免 CoreFoundation 对象泄漏
                try {
                    // 使用 CFData API 提取字节，避免 CF->NS 桥接问题
                    val bytePtr = CFDataGetBytePtr(cfData)
                    val length = CFDataGetLength(cfData)
                    if (bytePtr == null || length <= 0L) {
                        return null
                    }

                    // 从 C 指针读取字节数组
                    val bytes = bytePtr.readBytes(length.toInt())

                    // 将字节数组转换为 UTF-8 字符串
                    String(bytes, Charsets.UTF_8)
                } finally {
                    // 无论读取成功与否都释放 CFData，避免内存泄漏
                    CFRelease(cfData)
                }
            }
        } catch (_: Exception) {
            // Keychain 读取失败时静默返回 null，调用方会据此判断未登录状态
            null
        }
    }

    /**
     * 从 Keychain 删除指定条目
     *
     * @param key 条目名称（如 "access_token"）
     */
    private fun deleteFromKeychain(key: String) {
        val query = mutableMapOf<Any?, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to service,
            kSecAttrAccount to key
        )
        SecItemDelete(query as NSDictionary)
    }
}
