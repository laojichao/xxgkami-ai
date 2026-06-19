package com.xxgkami.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.xxgkami.android.data.TokenStore
import com.xxgkami.android.ui.theme.XXGKamiTheme
import com.xxgkami.android.navigation.AppNavigation
import com.xxgkami.android.util.RootDetector
import kotlinx.coroutines.launch

/**
 * 应用主Activity入口
 * 负责初始化TokenStore、检测 Root 环境、启用边到边显示，并设置Compose内容
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 在生命周期协程中异步初始化 TokenStore，避免阻塞主线程
        // TokenStore.init 涉及 EncryptedSharedPreferences 的密钥派生和文件 I/O
        lifecycleScope.launch {
            TokenStore.init(this@MainActivity)
        }
        enableEdgeToEdge()     // 启用边到边显示（沉浸式状态栏）
        val isRooted = RootDetector.isDeviceRooted()
        setContent {
            XXGKamiTheme {
                if (isRooted) {
                    RootWarningDialog()
                } else {
                    AppNavigation()
                }
            }
        }
    }
}

/**
 * Root 环境警告弹窗
 * 检测到设备已 root 时显示安全警告，用户必须确认后才能继续使用。
 *
 * TODO: 未来应结合 SafetyNet/Play Integrity API 进行完整性校验，
 * 对敏感操作（如支付、Token 刷新）在 root 设备上显示更严格警告或限制功能。
 * 当前仅显示警告，不阻止使用，避免误判导致正常用户无法使用。
 */
@Composable
private fun RootWarningDialog() {
    var dismissed by remember { mutableStateOf(false) }
    if (!dismissed) {
        AlertDialog(
            onDismissRequest = { /* 不允许点击外部关闭 */ },
            title = { Text("安全警告") },
            text = { Text("检测到您的设备已获得 Root 权限。在 Root 设备上使用本应用可能存在安全风险，卡密和账户信息可能被恶意程序窃取。建议在非 Root 设备上使用。") },
            confirmButton = {
                TextButton(onClick = { dismissed = true }) {
                    Text("我已知晓，继续使用")
                }
            }
        )
    } else {
        AppNavigation()
    }
}
