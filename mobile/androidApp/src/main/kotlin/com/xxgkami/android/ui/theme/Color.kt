/**
 * 应用颜色常量定义
 * 定义了主题所需的主色、辅色、背景色、错误色等基础颜色
 * 支持浅色和暗色两套配色方案
 */
package com.xxgkami.android.ui.theme

import androidx.compose.ui.graphics.Color

// 主色调 - 蓝色
val Primary = Color(0xFF409EFF)
val PrimaryDark = Color(0xFF337ECC)      // 主色深色变体
val Secondary = Color(0xFF67C23A)        // 辅助色 - 绿色
val Background = Color(0xFFF5F7FA)       // 页面背景色 - 浅灰
val Surface = Color(0xFFFFFFFF)          // 卡片/表面颜色 - 白色
val Error = Color(0xFFF56C6C)            // 错误状态颜色 - 红色
val OnPrimary = Color(0xFFFFFFFF)        // 主色上的文字颜色 - 白色
val OnBackground = Color(0xFF303133)     // 背景上的文字颜色 - 深灰
val OnSurface = Color(0xFF303133)        // 表面上的文字颜色 - 深灰
val TextSecondary = Color(0xFF909399)    // 次要文字颜色 - 浅灰
val Border = Color(0xFFDCDFE6)           // 边框颜色

// 暗色主题配色
val DarkPrimary = Color(0xFF66B1FF)
val DarkSecondary = Color(0xFF85CE61)
val DarkBackground = Color(0xFF1A1A1A)
val DarkSurface = Color(0xFF2C2C2C)
val DarkError = Color(0xFFF89898)
val DarkOnPrimary = Color(0xFF000000)
val DarkOnBackground = Color(0xFFE0E0E0)
val DarkOnSurface = Color(0xFFE0E0E0)
val DarkTextSecondary = Color(0xFFAAAAAA)
val DarkBorder = Color(0xFF4C4C4C)
