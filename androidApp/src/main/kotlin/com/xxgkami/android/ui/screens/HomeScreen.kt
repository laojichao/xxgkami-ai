package com.xxgkami.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("XXG卡密系统") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("小小怪卡密验证系统", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("专业的卡密验证解决方案", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))
            Button(onClick = { navController.navigate("verify") }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("验证卡密")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { navController.navigate("login") }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("用户登录")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { navController.navigate("register") }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("用户注册")
            }
        }
    }
}
