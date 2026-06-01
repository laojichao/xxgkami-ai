package com.xxgkami.shared

/**
 * Android平台actual实现
 * 返回Android系统版本号（SDK_INT）
 */
actual class Platform actual constructor() {
    actual val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}
