package com.xxgkami.shared

/**
 * Desktop平台actual实现
 * 返回桌面操作系统名称（如 Windows、Linux、Mac OS X）
 */
actual class Platform actual constructor() {
    actual val name: String = "Desktop ${System.getProperty("os.name")}"
}
