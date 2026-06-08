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
import com.xxgkami.android.data.TokenStore
import com.xxgkami.android.ui.screens.*

/**
 * 底部导航栏页面定义
 * 使用 sealed class 确保类型安全，每个页面包含路由路径、标题和图标
 *
 * @param route 路由路径
 * @param title 底部导航栏显示的标题
 * @param icon 底部导航栏显示的图标
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Cards : Screen("cards", "卡密", Icons.Default.CreditCard)
    data object Orders : Screen("orders", "订单", Icons.Default.List)
    data object Wallet : Screen("wallet", "钱包", Icons.Default.AccountBalanceWallet)
    data object Profile : Screen("profile", "我的", Icons.Default.Person)
}

/**
 * 主页面框架 - 包含底部导航栏的页面容器
 * 登录后进入此页面，包含卡密、订单、钱包、个人资料四个底部Tab
 *
 * @param onLogout 退出登录回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onLogout: () -> Unit) {
    val navController = rememberNavController()
    // 底部导航栏的四个Tab页面
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
                            // 导航时避免重复创建相同页面实例，并保存/恢复各Tab的状态
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

/**
 * 应用根导航组件
 * 管理全局页面路由，包括首页、登录、注册、验证和主页面
 * 通过 [isLoggedIn] 状态控制登录前后的页面切换
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // 全局登录状态，控制页面路由切换
    // 从 TokenStore 恢复登录状态，防止进程死亡后丢失登录态
    var isLoggedIn by remember { mutableStateOf(TokenStore.isLoggedIn()) }

    // 根据登录状态选择起始目的地，避免启动时闪屏
    val startDestination = if (isLoggedIn) "main" else "home"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("home") { HomeScreen(navController) }
        // 登录成功后清除回退栈并跳转到主页
        composable("login") { LoginScreen(navController, onLoginSuccess = { isLoggedIn = true; navController.navigate("main") { popUpTo(0) { inclusive = true } } }) }
        composable("register") { RegisterScreen(navController) }
        composable("verify") { VerifyScreen(navController) }
        composable("main") { MainScreen(onLogout = { isLoggedIn = false; navController.navigate("home") { popUpTo(0) { inclusive = true } } }) }
    }
}
