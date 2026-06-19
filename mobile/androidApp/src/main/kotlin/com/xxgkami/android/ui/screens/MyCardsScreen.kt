package com.xxgkami.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xxgkami.android.viewmodel.AuthViewModel
import com.xxgkami.android.viewmodel.CardViewModel
import com.xxgkami.shared.model.Card

/**
 * 我的卡密列表页面
 * 展示当前用户拥有的所有卡密信息，支持下拉刷新
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
    var isRefreshing by remember { mutableStateOf(false) }

    // 如果用户信息尚未加载（例如直接导航到此页面），先触发加载
    LaunchedEffect(Unit) {
        if (userInfo == null) {
            authViewModel.getUserInfo()
        }
    }
    // 用户信息加载完成后，自动加载该用户的卡密列表（userId 由服务端从 Token 提取）
    LaunchedEffect(userInfo?.id) {
        if (userInfo?.id != null) {
            viewModel.loadUserCards()
        }
    }

    // 监听加载状态，加载完成后关闭下拉刷新指示器
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isRefreshing = false
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("我的卡密") }) }) { padding ->
        when {
            isLoading && !isRefreshing -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                CardErrorState(
                    message = error ?: "加载失败",
                    onRetry = { viewModel.loadUserCards(forceRefresh = true) },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.loadUserCards(forceRefresh = true)
                    },
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    if (cards.isEmpty()) {
                        CardEmptyState()
                    } else {
                        CardList(cards = cards, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

/**
 * 卡密列表内容
 * @param cards 卡密数据列表
 * @param viewModel CardViewModel，用于解绑操作
 */
@Composable
private fun CardList(cards: List<Card>, viewModel: CardViewModel) {
    var unbindTarget by remember { mutableStateOf<Card?>(null) }

    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        items(cards, key = { card -> card.id ?: card.hashCode() }) { card ->
            CardItemCard(
                card = card,
                onUnbind = { unbindTarget = card }
            )
        }
    }

    // 解绑确认对话框
    unbindTarget?.let { card ->
        AlertDialog(
            onDismissRequest = { unbindTarget = null },
            title = { Text("解绑设备") },
            text = { Text("确定要解绑卡密 ${card.cardKey ?: ""} 的设备绑定吗？解绑后可在新设备上使用。") },
            confirmButton = {
                TextButton(onClick = {
                    card.cardKey?.let { key -> viewModel.unbindMachineCode(key) }
                    unbindTarget = null
                }) { Text("确认解绑") }
            },
            dismissButton = {
                TextButton(onClick = { unbindTarget = null }) { Text("取消") }
            }
        )
    }
}

/**
 * 单个卡密卡片
 * 展示卡密号、类型和状态，支持自助解绑操作
 * @param card 卡密数据对象
 * @param onUnbind 解绑按钮点击回调
 */
@Composable
private fun CardItemCard(card: Card, onUnbind: () -> Unit = {}) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("卡密: ${card.cardKey ?: ""}", style = MaterialTheme.typography.bodyMedium)
                    Text("类型: ${card.cardType ?: "未知"} | 状态: ${statusText(card.status)}", style = MaterialTheme.typography.bodySmall)
                    if (card.machineCode != null) {
                        Text("已绑定设备", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                // 仅在卡密已绑定机器码时显示解绑按钮
                if (card.machineCode != null) {
                    IconButton(onClick = onUnbind) {
                        Icon(Icons.Default.LinkOff, contentDescription = "解绑设备", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * 卡密空状态提示
 */
@Composable
private fun CardEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("暂无卡密", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 卡密加载错误状态
 * @param message 错误提示信息
 * @param onRetry 重试回调
 * @param modifier 修饰符
 */
@Composable
private fun CardErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text("重试")
            }
        }
    }
}

// 卡密状态码映射为中文
private fun statusText(status: Int?): String = when (status) {
    0 -> "未使用"
    1 -> "已使用"
    2 -> "已停用"
    else -> "未知"
}
