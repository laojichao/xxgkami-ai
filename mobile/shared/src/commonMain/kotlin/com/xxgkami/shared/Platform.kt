package com.xxgkami.shared

/**
 * 平台expect声明
 * 在 commonMain 中声明平台相关的类，各平台模块（Android、Desktop、iOS）提供 actual 实现
 * 用于获取当前运行平台的名称信息
 */
expect class Platform() {
    /** 当前平台名称标识 */
    val name: String
}
