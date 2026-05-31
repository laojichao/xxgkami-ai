package com.xxgkami.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.xxgkami.android.data.TokenStore
import com.xxgkami.android.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, authViewModel: AuthViewModel = viewModel(), onLogout: () -> Unit = {}) {
    val userInfo by authViewModel.userInfo.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.getUserInfo()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("个人资料") })
    }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text(userInfo?.username ?: "", style = MaterialTheme.typography.headlineSmall)
                    Text(userInfo?.email ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    ProfileItem("用户名", userInfo?.username ?: "")
                    ProfileItem("昵称", userInfo?.nickname ?: "未设置")
                    ProfileItem("邮箱", userInfo?.email ?: "未设置")
                    ProfileItem("手机号", userInfo?.phone ?: "未设置")
                    ProfileItem("角色", if (userInfo?.role == "admin") "管理员" else "普通用户")
                    ProfileItem("注册时间", userInfo?.createTime ?: "")
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val userId = userInfo?.id ?: 0
                    val role = userInfo?.role ?: "user"
                    authViewModel.logout(userId, role)
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("退出登录")
            }
        }
    }
}

@Composable
private fun ProfileItem(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
    if (label != "注册时间") {
        HorizontalDivider()
    }
}
