package com.xxgkami.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.xxgkami.android.ui.screens.*

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("login") { LoginScreen(navController) }
        composable("register") { RegisterScreen(navController) }
        composable("verify") { VerifyScreen(navController) }
        composable("cards") { MyCardsScreen(navController) }
        composable("orders") { OrdersScreen(navController) }
        composable("wallet") { WalletScreen(navController) }
        composable("profile") { ProfileScreen(navController) }
    }
}
