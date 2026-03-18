package com.campuslink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campuslink.domain.model.NetworkStats
import com.campuslink.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val users by viewModel.users.collectAsState()
    val stats by viewModel.networkStats.collectAsState()
    val isRunning by viewModel.isBluetoothRunning.collectAsState()
    val myId by viewModel.myUserId.collectAsState()

    val otherUsers = users.filter { it.userId != myId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CampusLink", fontWeight = FontWeight.Bold, color = Color.White) },
                actions = {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(if (isRunning) "● Bluetooth ON" else "● Bluetooth OFF", fontSize = 12.sp) },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isRunning) Color(0xFFDCFCE7) else Color(0xFFFFE4E6)
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B3A6B))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::refreshScan,
                containerColor = Color(0xFF1B3A6B)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Scan", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stats row
            StatsRow(stats, modifier = Modifier.padding(16.dp))

            Text(
                text = "Nearby Students",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFF0F172A)
            )

            if (otherUsers.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF1B3A6B))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Scanning for nearby students...", color = Color(0xFF64748B))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp, vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(otherUsers) { user ->
                        UserCard(user = user, onClick = {
                            navController.navigate("chat/${user.userId}/${user.username}")
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun StatsRow(stats: NetworkStats, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Nodes", stats.activeNodes.toString(), Color(0xFF1B3A6B), Modifier.weight(1f))
        StatCard("Sent", stats.messagesSent.toString(), Color(0xFF0F766E), Modifier.weight(1f))
        StatCard("Relayed", stats.messagesRelayed.toString(), Color(0xFF7C3AED), Modifier.weight(1f))
        StatCard("Delivered", stats.messagesDelivered.toString(), Color(0xFF16A34A), Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 10.sp, color = Color(0xFF64748B))
        }
    }
}

@Composable
fun UserCard(user: User, onClick: () -> Unit) {
    val avatarColors = listOf(
        Color(0xFF1B3A6B), Color(0xFF0F766E), Color(0xFF7C3AED),
        Color(0xFFDC2626), Color(0xFFD97706)
    )
    val avatarColor = avatarColors[user.userId.hashCode().and(0x7FFFFFFF) % avatarColors.size]

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (user.isOnline) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(user.username, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Text("@${user.userId}", fontSize = 12.sp, color = Color(0xFF64748B))
            }

            TextButton(onClick = onClick) {
                Text("Chat", color = Color(0xFF1B3A6B), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
