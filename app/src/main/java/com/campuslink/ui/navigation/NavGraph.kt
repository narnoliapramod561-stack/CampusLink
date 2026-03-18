package com.campuslink.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.campuslink.ui.auth.AuthScreen
import com.campuslink.ui.chat.ChatScreen
import com.campuslink.ui.home.HomeScreen

@Composable
fun NavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = "auth") {
        composable("auth") {
            AuthScreen(navController)
        }
        composable("home") {
            HomeScreen(navController)
        }
        composable("chat/{userId}/{username}") { backStackEntry ->
            ChatScreen(
                navController = navController,
                userId = backStackEntry.arguments?.getString("userId") ?: "",
                username = backStackEntry.arguments?.getString("username") ?: ""
            )
        }
    }
}
