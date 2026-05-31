package com.xxgkami.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.xxgkami.android.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel = viewModel(), onLoginSuccess: () -> Unit = {}) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val isLoading by viewModel.isLoading.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    LaunchedEffect(loginState) {
        if (loginState?.success == true) onLoginSuccess()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("登录") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("用户名") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Button(onClick = { viewModel.login(username, password) }, enabled = !isLoading, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("登录")
            }
            loginState?.let { if (!it.success) Text(it.message ?: "登录失败", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}
