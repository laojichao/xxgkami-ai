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
    // 用户信息加载完成后，自动加载该用户的卡密列表
    LaunchedEffect(userInfo) {
        userInfo?.id?.let { viewModel.loadUserCards(it) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("我的卡密") }) }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(cards) { card ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("卡密: ${card.cardKey ?: ""}", style = MaterialTheme.typography.bodyMedium)
                        Text("类型: ${card.cardType} | 状态: ${card.status}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
