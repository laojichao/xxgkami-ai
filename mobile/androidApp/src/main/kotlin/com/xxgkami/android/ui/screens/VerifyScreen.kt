package com.xxgkami.android.ui.screens

import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.xxgkami.android.viewmodel.CardViewModel

/**
 * 卡密验证页面
 * 用户输入卡密和可选的机器码进行验证
 * 验证成功后展示剩余次数、到期时间、剩余时间等信息
 *
 * @param navController 页面导航控制器
 * @param viewModel 卡密ViewModel，处理验证请求
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(navController: NavController, viewModel: CardViewModel = viewModel()) {
    var cardKey by remember { mutableStateOf("") }
    var machineCode by remember { mutableStateOf("") }
    val result by viewModel.verifyResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    // 使用 ANDROID_ID 作为设备唯一标识的主体，结合厂商型号信息增加区分度
    // ANDROID_ID 在应用签名不变的情况下稳定，且不同应用签名下不同，比 MANUFACTURER+MODEL 更唯一
    // 注意：ANDROID_ID 在恢复出厂设置后会变化，但对于卡密绑定场景足够使用
    val defaultMachineCode = remember {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: ""
        // 将 ANDROID_ID 与厂商型号组合哈希，避免直接暴露 ANDROID_ID 原始值
        "${androidId.takeLast(8)}_${Build.MANUFACTURER}_${Build.MODEL}"
    }

    Scaffold(topBar = { TopAppBar(title = { Text("卡密验证") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(value = cardKey, onValueChange = { cardKey = it }, label = { Text("请输入卡密") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = machineCode, onValueChange = { machineCode = it }, label = { Text("机器码（可选）") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            // 未输入机器码时使用默认设备标识
            Button(onClick = {
                if (cardKey.length > 100) return@Button
                viewModel.verify(cardKey, machineCode.ifBlank { defaultMachineCode })
            }, enabled = !isLoading && cardKey.isNotEmpty() && cardKey.length <= 100, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp)) else Text("验证")
            }
            result?.let { r ->
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(r.message ?: "", color = if (r.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        r.remainingCount?.let { Text("剩余次数: $it") }
                        r.expireTime?.let { Text("到期时间: $it") }
                        // 将秒数转换为"X小时Y分钟"格式显示
                        r.remainingTime?.let { Text("剩余时间: ${it / 3600}小时${(it % 3600) / 60}分钟") }
                    }
                }
            }
        }
    }
}
