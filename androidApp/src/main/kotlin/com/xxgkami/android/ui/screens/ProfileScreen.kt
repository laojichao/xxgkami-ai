package com.xxgkami.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    Scaffold(topBar = { TopAppBar(title = { Text("个人资料") }) }) { padding ->
        Box(modifier = androidx.compose.ui.Modifier.padding(padding)) { Text("个人资料", modifier = androidx.compose.ui.Modifier.padding(16.dp)) }
    }
}
