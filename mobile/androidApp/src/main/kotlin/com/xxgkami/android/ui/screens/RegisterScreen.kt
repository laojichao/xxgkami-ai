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

    // 注册成功后自动返回上一页
    LaunchedEffect(registerSuccess) {
        if (registerSuccess) {
            navController.popBackStack()
        }
    }

    // 输入校验：邮箱必须包含 @ 且 @ 在 . 之前，且前后都有内容
    val isEmailValid = email.let { e ->
        val atIdx = e.indexOf('@')
        val dotIdx = e.lastIndexOf('.')
        atIdx > 0 && dotIdx > atIdx + 1 && dotIdx < e.length - 1
    }
    val isFormValid = username.isNotBlank() && isEmailValid && code.isNotBlank() && password.length >= 6

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
                    onClick = { viewModel.sendCode(email) },
                    enabled = !isLoading && email.isNotBlank() && isEmailValid
                ) { Text("发送") }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("密码（至少6位）") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = password.isNotEmpty() && password.length < 6
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
