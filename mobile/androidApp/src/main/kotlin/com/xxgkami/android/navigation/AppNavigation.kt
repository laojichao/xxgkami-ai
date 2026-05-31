package com.xxgkami.android.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.xxgkami.android.ui.screens.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Cards : Screen("cards", "卡密", Icons.Default.CreditCard)
    data object Orders : Screen("orders", "订单", Icons.Default.List)
    data object Wallet : Screen("wallet", "钱包", Icons.Default.AccountBalanceWallet)
    data object Profile : Screen("profile", "我的", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    val bottomNavItems = listOf(Screen.Cards, Screen.Orders, Screen.Wallet, Screen.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Cards.route, Modifier.padding(innerPadding)) {
            composable(Screen.Cards.route) { MyCardsScreen(navController) }
            composable(Screen.Orders.route) { OrdersScreen(navController) }
            composable(Screen.Wallet.route) { WalletScreen(navController) }
            composable(Screen.Profile.route) { ProfileScreen(navController, onLogout = onLogout) }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    var isLoggedIn by remember { mutableStateOf(false) }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("login") { LoginScreen(navController, onLoginSuccess = { isLoggedIn = true; navController.navigate("main") { popUpTo(0) { inclusive = true } } }) }
        composable("register") { RegisterScreen(navController) }
        composable("verify") { VerifyScreen(navController) }
        composable("main") { MainScreen(onLogout = { isLoggedIn = false; navController.navigate("home") { popUpTo(0) { inclusive = true } } }) }
    }
}
