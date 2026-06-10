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
import com.xxgkami.android.viewmodel.OrderViewModel
import com.xxgkami.shared.model.Order

/**
 * 订单列表页面
 * 展示当前用户的所有订单，支持下拉刷新
 *
 * @param navController 页面导航控制器
 * @param orderViewModel 订单ViewModel，负责加载订单数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(navController: NavController, orderViewModel: OrderViewModel = viewModel()) {
    val orders by orderViewModel.orders.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()
    val error by orderViewModel.error.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    // 页面首次加载时请求订单数据
    LaunchedEffect(Unit) {
        orderViewModel.loadOrders()
    }

    // 监听加载状态，加载完成后关闭下拉刷新指示器
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isRefreshing = false
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("我的订单") })
    }) { padding ->
        when {
            isLoading && !isRefreshing -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null && orders.isEmpty() -> {
                OrderErrorState(
                    message = error ?: "加载失败",
                    onRetry = { orderViewModel.loadOrders(forceRefresh = true) },
                    modifier = Modifier.padding(padding)
                )
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        orderViewModel.loadOrders(forceRefresh = true)
                    },
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    if (orders.isEmpty()) {
                        OrderEmptyState()
                    } else {
                        OrderList(orders = orders)
                    }
                }
            }
        }
    }
}

/**
 * 订单列表内容
 * @param orders 订单数据列表
 */
@Composable
private fun OrderList(orders: List<Order>) {
    LazyColumn(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        items(orders, key = { order -> order.id ?: order.hashCode() }) { order ->
            OrderCard(order = order)
        }
    }
}

/**
 * 单个订单卡片
 * 展示订单号、卡密类型、数量、总价、状态和创建时间
 * @param order 订单数据对象
 */
@Composable
private fun OrderCard(order: Order) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("订单号: ${order.orderNo ?: ""}", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text("类型: ${order.cardType ?: ""} | 规格: ${order.cardSpec ?: ""}", style = MaterialTheme.typography.bodyMedium)
            Text("数量: ${order.quantity ?: 0} | 总价: ¥${order.totalPrice ?: "0"}", style = MaterialTheme.typography.bodyMedium)
            Text("状态: ${orderStatusText(order.status)} | 支付方式: ${order.paymentMethod ?: ""}", style = MaterialTheme.typography.bodySmall)
            order.createTime?.let {
                Text("创建时间: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * 订单空状态提示
 */
@Composable
private fun OrderEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("暂无订单", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 订单加载错误状态
 * @param message 错误提示信息
 * @param onRetry 重试回调
 * @param modifier 修饰符
 */
@Composable
private fun OrderErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
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

// 订单状态映射为中文
private fun orderStatusText(status: String?): String = when (status) {
    "pending" -> "待支付"
    "completed" -> "已完成"
    "failed" -> "已失败"
    "paid" -> "已支付"
    "cancelled" -> "已取消"
    "refunded" -> "已退款"
    else -> status ?: "未知"
}
