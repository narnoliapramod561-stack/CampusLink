package com.campuslink.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.campuslink.ui.theme.*
import com.campuslink.ui.auth.AuthScreen
import com.campuslink.ui.home.HomeScreen
import com.campuslink.ui.nearby.NearbyScreen
import com.campuslink.ui.analytics.AnalyticsScreen
import com.campuslink.ui.settings.SettingsScreen
import com.campuslink.ui.chat.ChatScreen
import com.campuslink.ui.broadcast.BroadcastScreen
import com.campuslink.ui.groups.GroupScreen

sealed class Screen(val route: String, val icon: ImageVector? = null, val label: String? = null) {
    object Auth : Screen("auth")
    object Home : Screen("home", Icons.Default.ChatBubble, "Chats")
    object Campus : Screen("campus", Icons.Default.Campaign, "Campus")
    object Nearby : Screen("nearby", Icons.Default.WifiTethering, "Nearby")
    object Analytics : Screen("analytics", Icons.Default.BarChart, "Network")
    object Settings : Screen("settings", Icons.Default.Settings, "Settings")
    
    object Groups : Screen("groups")
    object Chat : Screen("chat/{userId}") { fun createRoute(id: String) = "chat/$id" }
    object GroupChat : Screen("group_chat/{groupId}") { fun createRoute(id: String) = "group_chat/$id" }
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) { AuthScreen(onLoginSuccess = { navController.navigate(Screen.Home.route) { popUpTo(0) } }) }
        composable(Screen.Home.route) { MainShell(navController) { HomeScreen(it) } }
        composable(Screen.Campus.route) { MainShell(navController) { BroadcastScreen() } }
        composable(Screen.Nearby.route) { MainShell(navController) { NearbyScreen(it) } }
        composable(Screen.Analytics.route) { MainShell(navController) { AnalyticsScreen(it) } }
        composable(Screen.Settings.route) { MainShell(navController) { SettingsScreen(it) } }
        
        composable(Screen.Groups.route) {
            GroupScreen(onOpenChat = { groupId -> navController.navigate(Screen.GroupChat.createRoute(groupId)) })
        }
        composable(Screen.Chat.route) { backStack ->
            ChatScreen(
                userId = backStack.arguments?.getString("userId") ?: "",
                targetType = com.campuslink.domain.model.MessageTargetType.USER.name,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.GroupChat.route) { backStack ->
            ChatScreen(
                userId = backStack.arguments?.getString("groupId") ?: "",
                targetType = com.campuslink.domain.model.MessageTargetType.GROUP.name,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun MainShell(navController: NavHostController, content: @Composable (NavHostController) -> Unit) {
    val items = listOf(Screen.Home, Screen.Campus, Screen.Nearby, Screen.Analytics, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = WarmBlack, tonalElevation = 0.dp) {
                items.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = { navController.navigate(screen.route) { launchSingleTop = true } },
                        icon = { Icon(screen.icon!!, null) },
                        label = { Text(screen.label!!, fontSize = 9.sp) }, // Smaller font to fit 5 items
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = TealAccent,
                            selectedTextColor = TealAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = WarmBlack
                        )
                    )
                }
            }
        },
        containerColor = WarmBlack
    ) { padding ->
        Box(Modifier.padding(padding)) { content(navController) }
    }
}
