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

/**
 * 订单列表页面
 * 展示当前用户的所有订单，包括订单号、卡密类型、数量、总价、状态等信息
 * 无订单时显示空状态提示
 *
 * @param navController 页面导航控制器
 * @param orderViewModel 订单ViewModel，负责加载订单数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(navController: NavController, orderViewModel: OrderViewModel = viewModel()) {
    val orders by orderViewModel.orders.collectAsState()

    // 页面首次加载时请求订单数据
    LaunchedEffect(Unit) {
        orderViewModel.loadOrders()
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("我的订单") })
    }) { padding ->
        if (orders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("暂无订单", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                items(orders) { order ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("订单号: ${order.orderNo ?: ""}", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text("类型: ${order.cardType ?: ""} | 规格: ${order.cardSpec ?: ""}", style = MaterialTheme.typography.bodyMedium)
                            Text("数量: ${order.quantity ?: 0} | 总价: ¥${order.totalPrice ?: "0"}", style = MaterialTheme.typography.bodyMedium)
                            Text("状态: ${order.status ?: ""} | 支付方式: ${order.paymentMethod ?: ""}", style = MaterialTheme.typography.bodySmall)
                            order.createTime?.let {
                                Text("创建时间: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
