package com.xxgkami.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.xxgkami.android.data.TokenStore
import com.xxgkami.android.ui.theme.XXGKamiTheme
import com.xxgkami.android.navigation.AppNavigation
import com.xxgkami.android.util.RootDetector
import com.xxgkami.shared.api.ApiProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 应用主Activity入口
 * 负责初始化API基础地址与TokenStore、检测 Root 环境、启用边到边显示，并设置Compose内容
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 尽早初始化 API 基础地址，必须在任何网络请求之前完成。
        // debug 默认为模拟器地址 10.0.2.2；release 构建时应通过
        // -PapiBaseUrl=https://... 覆盖（见 androidApp/build.gradle.kts 中 API_BASE_URL）。
        ApiProvider.updateBaseUrl(BuildConfig.API_BASE_URL)
        enableEdgeToEdge()     // 启用边到边显示（沉浸式状态栏）

        // 状态驱动：TokenStore 异步初始化完成前显示加载占位。
        // 若在 init 完成前组合 AppNavigation，TokenStore.isLoggedIn() 会读取到
        // 未初始化的 prefs（null），导致已登录用户冷启动被误判为未登录。
        var ready by mutableStateOf(false)
        // Root 检测结果：null 表示尚未检测完成（检测涉及文件 I/O，必须移出主线程）
        var isRooted by mutableStateOf<Boolean?>(null)

        lifecycleScope.launch {
            // TokenStore.init（内部已切 IO 线程）与 Root 检测并行执行
            val tokenJob = launch { TokenStore.init(this@MainActivity) }
            val rootJob = launch(Dispatchers.IO) { isRooted = RootDetector.isDeviceRooted() }
            tokenJob.join()
            rootJob.join()
            ready = true
        }

        setContent {
            XXGKamiTheme {
                when {
                    !ready -> {
                        // 初始化期间的简单加载占位
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    isRooted == true -> RootWarningDialog()
                    else -> AppNavigation()
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
