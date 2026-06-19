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
import com.xxgkami.shared.model.Wallet
import com.xxgkami.shared.model.WalletTransaction

/**
 * 钱包管理页面
 * 展示用户余额、累计充值、累计消费信息和交易记录，支持下拉刷新
 *
 * @param navController 页面导航控制器
 * @param walletViewModel 钱包ViewModel，负责加载钱包和交易记录数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(navController: NavController, walletViewModel: WalletViewModel = viewModel()) {
    val wallet by walletViewModel.wallet.collectAsState()
    val transactions by walletViewModel.transactions.collectAsState()
    val isLoading by walletViewModel.isLoading.collectAsState()
    val error by walletViewModel.error.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    // 页面首次加载时并行请求钱包信息和交易记录
    LaunchedEffect(Unit) {
        walletViewModel.loadWallet()
        walletViewModel.loadTransactions()
    }

    // 监听加载状态，加载完成后关闭下拉刷新指示器
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isRefreshing = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("钱包") })
    }) { padding ->
        when {
            isLoading && wallet == null && !isRefreshing -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null && wallet == null -> {
                WalletErrorState(
                    message = error ?: "加载失败",
                    onRetry = {
                        walletViewModel.loadWallet(forceRefresh = true)
                        walletViewModel.loadTransactions(forceRefresh = true)
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        walletViewModel.loadWallet(forceRefresh = true)
                        walletViewModel.loadTransactions(forceRefresh = true)
                    },
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    WalletContent(
                        wallet = wallet,
                        transactions = transactions,
                        onRecharge = { amount -> walletViewModel.recharge(amount) }
                    )
                }
            }
        }
    }
}

/**
 * 钱包页面主内容区域
 * 包含余额卡片和交易记录列表
 * @param wallet 钱包信息，可能为null
 * @param transactions 交易记录列表
 * @param onRecharge 充值回调
 */
@Composable
private fun WalletContent(wallet: Wallet?, transactions: List<WalletTransaction>, onRecharge: (Double) -> Unit = {}) {
    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        item {
            WalletBalanceCard(wallet = wallet, onRecharge = onRecharge)
            Spacer(Modifier.height(16.dp))
            Text("交易记录", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
        }

        if (transactions.isEmpty()) {
            item {
                TransactionEmptyState()
            }
        } else {
            items(transactions, key = { tx -> tx.id ?: tx.hashCode() }) { tx ->
                TransactionCard(transaction = tx)
            }
        }
    }
}

/**
 * 钱包余额概览卡片
 * 展示当前余额、累计充值和累计消费，支持充值操作
 * @param wallet 钱包信息，可能为null
 * @param onRecharge 充值回调，传入充值金额
 */
@Composable
private fun WalletBalanceCard(wallet: Wallet?, onRecharge: ((Double) -> Unit)? = null) {
    var showRechargeDialog by remember { mutableStateOf(false) }

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
            if (onRecharge != null) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showRechargeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("充值")
                }
            }
        }
    }

    // 充值金额输入对话框
    if (showRechargeDialog) {
        var amountText by remember { mutableStateOf("") }
        var amountError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showRechargeDialog = false },
            title = { Text("钱包充值") },
            text = {
                Column {
                    Text("请输入充值金额")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it; amountError = null },
                        label = { Text("金额（元）") },
                        isError = amountError != null,
                        supportingText = amountError?.let { { Text(it) } },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // 使用 ViewModel 提供的统一校验函数，确保 UI 与 ViewModel 校验逻辑一致
                    val validation = WalletViewModel.validateRechargeAmount(amountText)
                    if (validation != null) {
                        amountError = validation
                    } else {
                        // 校验通过后传递原始字符串，由 ViewModel 再次校验并提交
                        onRecharge?.invoke(amountText.trim().toDouble())
                        showRechargeDialog = false
                    }
                }) { Text("确认充值") }
            },
            dismissButton = {
                TextButton(onClick = { showRechargeDialog = false }) { Text("取消") }
            }
        )
    }
}

/**
 * 单条交易记录卡片
 * 消费显示红色负数，充值/其他显示绿色正数
 * @param transaction 交易记录数据
 */
@Composable
private fun TransactionCard(transaction: WalletTransaction) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(transaction.description ?: transaction.type ?: "", style = MaterialTheme.typography.bodyMedium)
                transaction.createTime?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                "${if (transaction.type == "consume") "-" else "+"}¥${transaction.amount ?: "0"}",
                style = MaterialTheme.typography.titleMedium,
                color = if (transaction.type == "consume") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * 交易记录空状态提示
 */
@Composable
private fun TransactionEmptyState() {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text("暂无交易记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 钱包加载错误状态
 * @param message 错误提示信息
 * @param onRetry 重试回调
 * @param modifier 修饰符
 */
@Composable
private fun WalletErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
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
