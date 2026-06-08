package com.xxgkami.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxgkami.android.viewmodel.AuthViewModel
import com.xxgkami.android.viewmodel.CardViewModel

/**
 * 我的卡密列表页面
 * 展示当前用户拥有的所有卡密信息，包括卡密号、类型和状态
 *
 * @param navController 页面导航控制器
 * @param viewModel 卡密ViewModel，负责加载卡密数据
 * @param authViewModel 认证ViewModel，提供当前用户信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyCardsScreen(navController: NavController, viewModel: CardViewModel = viewModel(), authViewModel: AuthViewModel = viewModel()) {
    val cards by viewModel.cards.collectAsState()
    val userInfo by authViewModel.userInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // 如果用户信息尚未加载（例如直接导航到此页面），先触发加载
    LaunchedEffect(Unit) {
        if (userInfo == null) {
            authViewModel.getUserInfo()
        }
    }
    // 用户信息加载完成后，自动加载该用户的卡密列表（使用 id 避免不必要的重复触发）
    LaunchedEffect(userInfo?.id) {
        userInfo?.id?.let { viewModel.loadUserCards(it) }
    }

    // 卡密状态码映射为中文
    fun statusText(status: Int?): String = when (status) {
        0 -> "未使用"
        1 -> "已使用"
        2 -> "已停用"
        else -> "未知"
    }

    Scaffold(topBar = { TopAppBar(title = { Text("我的卡密") }) }) { padding ->
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(error ?: "加载失败", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { userInfo?.id?.let { viewModel.loadUserCards(it) } }) {
                            Text("重试")
                        }
                    }
                }
            }
            cards.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("暂无卡密", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                    items(cards, key = { card -> card.id ?: card.hashCode() }) { card ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("卡密: ${card.cardKey ?: ""}", style = MaterialTheme.typography.bodyMedium)
                                Text("类型: ${card.cardType ?: "未知"} | 状态: ${statusText(card.status)}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
