package com.xxgkami.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxgkami.android.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

/**
 * 用户注册页面
 * 支持通过用户名、邮箱验证码、密码完成注册
 * 包含发送邮箱验证码、输入校验、加载状态和错误提示
 *
 * @param navController 页面导航控制器
 * @param viewModel 认证ViewModel，处理注册和验证码发送逻辑
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController, viewModel: AuthViewModel = viewModel()) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val registerSuccess by viewModel.registerSuccess.collectAsState()

    // 验证码发送倒计时（秒），0 表示可发送
    var countdown by remember { mutableStateOf(0) }

    // 注册成功后自动返回上一页
    LaunchedEffect(registerSuccess) {
        if (registerSuccess) {
            navController.popBackStack()
        }
    }

    // 倒计时协程：每秒递减，到 0 停止
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    // 输入校验：使用更严格的邮箱正则，防止明显非法邮箱
    // 正则说明：local 部分允许字母数字及 +_.-，域名部分要求至少两段且 TLD 为 2-6 位字母
    val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?\\.[A-Za-z]{2,6}$")
    val isEmailValid = email.matches(emailRegex)
    val isFormValid = username.isNotBlank() && isEmailValid && code.isNotBlank() && password.length >= 8

    Scaffold(topBar = { TopAppBar(title = { Text("注册") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            // 错误提示
            error?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            OutlinedTextField(
                value = username, onValueChange = { username = it },
                label = { Text("用户名") }, modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("邮箱") }, modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = email.isNotEmpty() && !isEmailValid
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = code, onValueChange = { code = it },
                    label = { Text("验证码") }, modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.sendCode(email)
                        // 发送后启动 60 秒倒计时，防止频繁发送
                        countdown = 60
                    },
                    // 倒计时期间或加载中或邮箱无效时禁用按钮
                    enabled = !isLoading && countdown == 0 && email.isNotBlank() && isEmailValid
                ) {
                    // 倒计时期间显示剩余秒数，否则显示"发送"
                    Text(if (countdown > 0) "${countdown}s" else "发送")
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("密码（至少8位）") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = password.isNotEmpty() && password.length < 8
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.register(username, password, email, code) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                enabled = !isLoading && isFormValid
            ) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Text("注册")
            }
        }
    }
}
