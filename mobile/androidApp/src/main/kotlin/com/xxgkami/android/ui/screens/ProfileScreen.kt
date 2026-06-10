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

/**
 * 个人资料页面
 * 展示用户头像、用户名、邮箱、昵称、手机号、角色、注册时间等信息
 * 提供退出登录功能
 *
 * @param navController 页面导航控制器
 * @param authViewModel 认证ViewModel，提供用户信息和退出登录功能
 * @param onLogout 退出登录回调，用于跳转回首页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, authViewModel: AuthViewModel = viewModel(), onLogout: () -> Unit = {}) {
    val userInfo by authViewModel.userInfo.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.getUserInfo()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("个人资料") })
    }) { padding ->
        if (isLoading && userInfo == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null && userInfo == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error ?: "加载失败", color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { authViewModel.getUserInfo() }) {
                        Text("重试")
                    }
                }
            }
        } else {
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
                    // 修复：先发起服务端登出请求（此时 token 仍有效），再清理本地状态并导航
                    // 原代码先清理 token 导致服务端请求因无 token 而失败
                    authViewModel.logout(userId, role)
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("退出登录")
            }
        }
        } // end else
    }
}

/**
 * 个人资料信息行组件
 * 左侧显示标签名，右侧显示对应值，行间用分割线分隔
 *
 * @param label 标签名称（如"用户名"、"邮箱"等）
 * @param value 对应的值
 */
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
