package com.xxgkami.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xxgkami.android.data.TokenStore
import com.xxgkami.android.ui.theme.XXGKamiTheme
import com.xxgkami.android.navigation.AppNavigation

/**
 * 应用主Activity入口
 * 负责初始化TokenStore、启用边到边显示，并设置Compose内容
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TokenStore.init(this)  // 初始化持久化Token存储
        enableEdgeToEdge()     // 启用边到边显示（沉浸式状态栏）
        setContent {
            XXGKamiTheme {
                AppNavigation()
            }
        }
    }
}
