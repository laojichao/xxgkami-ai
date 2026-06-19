package com.xxgkami.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

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

// 暗色主题配色方案
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    secondary = DarkSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    error = DarkError,
    onBackground = DarkOnBackground,
    onSurface = DarkOnSurface,
    outline = DarkBorder,
)

/**
 * XXGKami 应用主题
 * 提供统一的 Material3 主题包装，自动跟随系统浅色/暗色模式切换
 *
 * Android 12+ 动态颜色处理：使用系统动态色但保留品牌主色（primary），
 * 避免动态颜色完全覆盖品牌识别色。
 *
 * @param darkTheme 是否使用暗色主题，默认跟随系统设置
 * @param content 主题包裹的内容
 */
@Composable
fun XXGKamiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 支持动态颜色（Material You），但保留品牌主色 primary
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            // 使用动态色但覆盖 primary 为品牌色，保持品牌识别度
            if (darkTheme) {
                dynamicDarkColorScheme(context).copy(primary = DarkPrimary)
            } else {
                dynamicLightColorScheme(context).copy(primary = Primary)
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
