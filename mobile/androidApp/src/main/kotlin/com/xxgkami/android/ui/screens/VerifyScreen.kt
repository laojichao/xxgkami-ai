package com.xxgkami.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.xxgkami.android.viewmodel.CardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyScreen(navController: NavController, viewModel: CardViewModel = viewModel()) {
    var cardKey by remember { mutableStateOf("") }
    var machineCode by remember { mutableStateOf("") }
    val result by viewModel.verifyResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("卡密验证") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedTextField(value = cardKey, onValueChange = { cardKey = it }, label = { Text("请输入卡密") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = machineCode, onValueChange = { machineCode = it }, label = { Text("机器码（可选）") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))
            Button(onClick = { viewModel.verify(cardKey, if (machineCode.isEmpty()) "Android" else machineCode) }, enabled = !isLoading && cardKey.isNotEmpty(), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                if (isLoading) CircularProgressIndicator(Modifier.size(24.dp)) else Text("验证")
            }
            result?.let { r ->
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(r.message ?: "", color = if (r.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        r.remainingCount?.let { Text("剩余次数: $it") }
                        r.expireTime?.let { Text("到期时间: $it") }
                        r.remainingTime?.let { Text("剩余时间: ${it / 3600}小时${(it % 3600) / 60}分钟") }
                    }
                }
            }
        }
    }
}
