package com.xxgkami.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.xxgkami.android.viewmodel.WalletViewModel

/**
 * 钱包管理页面
 * 展示用户余额、累计充值、累计消费信息
 * 下方列出交易记录列表，消费显示红色负数，充值显示绿色正数
 *
 * @param navController 页面导航控制器
 * @param walletViewModel 钱包ViewModel，负责加载钱包和交易记录数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(navController: NavController, walletViewModel: WalletViewModel = viewModel()) {
    val wallet by walletViewModel.wallet.collectAsState()
    val transactions by walletViewModel.transactions.collectAsState()

    // 页面首次加载时并行请求钱包信息和交易记录
    LaunchedEffect(Unit) {
        walletViewModel.loadWallet()
        walletViewModel.loadTransactions()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("钱包") })
    }) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("余额", style = MaterialTheme.typography.titleMedium)
                        Text("¥${wallet?.balance ?: "0.00"}", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("累计充值", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("¥${wallet?.totalRecharge ?: "0.00"}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text("累计消费", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("¥${wallet?.totalConsume ?: "0.00"}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("交易记录", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
            }

            if (transactions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无交易记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(transactions) { tx ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(tx.description ?: tx.type ?: "", style = MaterialTheme.typography.bodyMedium)
                                tx.createTime?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text(
                                // 消费显示负数（红色），充值/其他显示正数（绿色）
                            "${if (tx.type == "consume") "-" else "+"}¥${tx.amount ?: "0"}",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (tx.type == "consume") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
