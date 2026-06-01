package com.xxgkami.shared

import platform.UIKit.UIDevice

/**
 * iOS平台actual实现
 * 返回iOS系统名称和版本号（如 "iOS 17.0"）
 */
actual class Platform actual constructor() {
    actual val name: String = "${UIDevice.currentDevice.systemName()} ${UIDevice.currentDevice.systemVersion}"
}
