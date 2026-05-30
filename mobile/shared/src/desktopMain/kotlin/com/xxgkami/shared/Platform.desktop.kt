package com.xxgkami.shared

actual class Platform actual constructor() {
    actual val name: String = "Desktop ${System.getProperty("os.name")}"
}
