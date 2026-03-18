package com.campuslink.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val simState by viewModel.simState.collectAsState()

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

            // --- SIMULATION DEMO SECTION ---
            RelayDemoSection(simState, viewModel::startRelayDemo)
            
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Nearby Students",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = Color(0xFF0F172A)
            )

            if (otherUsers.isEmpty() && !simState.isRunning) {
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
                    contentPadding = PaddingValues(
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
fun RelayDemoSection(state: SimState, onStart: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF1B3A6B), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Multi-Hop Relay Demo", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.weight(1f))
                if (!state.isRunning) {
                    TextButton(onClick = onStart) {
                        Text("Start Simulation", color = Color(0xFF1B3A6B), fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Visualizer Canvas
            RelayVisualizer(state)

            Spacer(modifier = Modifier.height(16.dp))
            
            val statusText = when {
                !state.isRunning -> "Press Start to see A ➔ B ➔ C ➔ D relay"
                state.isAck -> "D ➔ C ➔ B ➔ A: Sending ACK back..."
                else -> "${state.activeNode} ➔ Next: Forwarding message..."
            }
            Text(
                text = statusText,
                fontSize = 12.sp,
                color = if (state.isRunning) Color(0xFF1B3A6B) else Color(0xFF64748B),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RelayVisualizer(state: SimState) {
    val nodeColor = Color(0xFFE2E8F0)
    val activeColor = Color(0xFF1B3A6B)
    val packetColor = if (state.isAck) Color(0xFF16A34A) else Color(0xFF7C3AED)

    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
            val width = size.width
            val centerY = size.height / 2
            
            // Draw Lines
            drawLine(
                color = nodeColor,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // Draw Nodes A, B, C, D
            val nodes = listOf(0f, 0.33f, 0.66f, 1.0f)
            val labels = listOf("A", "B", "C", "D")
            
            nodes.forEachIndexed { index, pos ->
                val x = pos * width
                val isActive = when(index) {
                    0 -> state.activeNode == SimNode.A
                    1 -> state.activeNode == SimNode.B
                    2 -> state.activeNode == SimNode.C
                    3 -> state.activeNode == SimNode.D
                    else -> false
                }
                
                drawCircle(
                    color = if (isActive) activeColor else nodeColor,
                    radius = 12.dp.toPx(),
                    center = Offset(x, centerY)
                )
                
                // Text labels would normally need native canvas access or separate Box, 
                // but we'll stick to a simple circle visual for the nodes.
            }

            // Draw Packet during animation
            if (state.isRunning && state.packetPos >= 0) {
                drawCircle(
                    color = packetColor,
                    radius = 8.dp.toPx(),
                    center = Offset(state.packetPos * width, centerY)
                )
            }
        }
        
        // Overlay node labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp - 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("A", "B", "C", "D").forEach { label ->
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
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
