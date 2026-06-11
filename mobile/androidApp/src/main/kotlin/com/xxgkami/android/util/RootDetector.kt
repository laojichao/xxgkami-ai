package com.xxgkami.android.util

import android.os.Build
import java.io.File

/**
 * Root 检测工具
 *
 * 通过多种方式检测设备是否已 root：
 * 1. 检查 su 二进制文件是否存在
 * 2. 检查常见 Root 管理应用的安装路径
 * 3. 检查 build tags 是否包含 test-keys
 *
 * 注意：此检测可被高级用户绕过（如 Magisk Hide），
 * 对于高安全场景应结合 SafetyNet/Play Integrity API。
 * 这里提供基础防护层，阻止普通用户在 root 设备上使用敏感功能。
 */
object RootDetector {

    /** 常见 su 二进制路径 */
    private val SU_PATHS = arrayOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    /** 常见 Root 管理应用目录 */
    private val ROOT_APP_PATHS = arrayOf(
        "/system/app/Superuser.apk",
        "/system/app/SuperSU.apk",
        "/system/app/com.topjohnwu.magisk",
        "/data/data/com.topjohnwu.magisk",
        "/data/data/eu.chainfire.supersu",
        "/data/data/com.noshufou.android.su"
    )

    /**
     * 综合检测设备是否已 root。
     *
     * @return true 表示检测到 root 环境
     */
    fun isDeviceRooted(): Boolean {
        return checkSuBinary() || checkRootApps() || checkTestKeys()
    }

    /** 检查 su 二进制文件是否存在且可执行 */
    private fun checkSuBinary(): Boolean {
        return try {
            SU_PATHS.any { path ->
                val file = File(path)
                file.exists() && file.canExecute()
            }
        } catch (_: Exception) {
            false
        }
    }

    /** 检查常见 Root 管理应用的安装路径 */
    private fun checkRootApps(): Boolean {
        return try {
            ROOT_APP_PATHS.any { path -> File(path).exists() }
        } catch (_: Exception) {
            false
        }
    }

    /** 检查系统 build tags 是否为 test-keys（通常表示自编译 ROM） */
    private fun checkTestKeys(): Boolean {
        return try {
            val tags = Build.TAGS
            tags != null && tags.contains("test-keys")
        } catch (_: Exception) {
            false
        }
    }
}
