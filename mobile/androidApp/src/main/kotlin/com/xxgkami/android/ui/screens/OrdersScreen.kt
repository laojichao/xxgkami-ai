package com.xxgkami.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(navController: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text("我的订单") }) }) { padding ->
        Box(modifier = androidx.compose.ui.Modifier.padding(padding)) { Text("订单列表", modifier = androidx.compose.ui.Modifier.padding(16.dp)) }
    }
}
