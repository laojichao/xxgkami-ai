package com.xxgkami.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 浅色主题配色方案，使用自定义颜色常量
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    secondary = Secondary,
    background = Background,
    surface = Surface,
    error = Error,
    onBackground = OnBackground,
    onSurface = OnSurface,
    outline = Border,
)

/**
 * XXGKami 应用主题
 * 提供统一的 Material3 浅色主题包装，所有页面应使用此主题
 *
 * @param content 主题包裹的内容
 */
@Composable
fun XXGKamiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
