package com.campuslink.ui.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.campuslink.ui.components.*
import com.campuslink.ui.navigation.Screen
import com.campuslink.ui.theme.*

@Composable
fun NearbyScreen(
    navController: NavHostController,
    viewModel: NearbyViewModel = hiltViewModel()
) {
    val users         by viewModel.users.collectAsState()
    val stats         by viewModel.stats.collectAsState()
    val isRunning     by viewModel.isRunning.collectAsState()
    val connectedCount by viewModel.connectedCount.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CL.BgDeep)
    ) {
        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Nearby", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (isRunning) "$connectedCount peer${if (connectedCount == 1) "" else "s"} in range"
                    else "Bluetooth off",
                    color = if (isRunning && connectedCount > 0) TealAccent else TextSecondary,
                    fontSize = 12.sp
                )
            }
            StatusBadge(
                text = if (isRunning) "SCANNING" else "OFFLINE",
                active = isRunning
            )
        }

        // ── Stats strip ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .glassCard(16)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NearbyStatItem("$connectedCount",                                           "In Range",  TealAccent)
            NearbyStatItem("${stats.messagesRelayed}",                                  "Relayed",   AmberGlow)
            NearbyStatItem("${String.format("%.1f", stats.avgHopCount)}",               "Avg Hops",  BlueAccent)
            NearbyStatItem("${(stats.relaySuccessRate * 100).toInt()}%",                "Success",   OnlineDot)
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("STUDENTS IN RANGE")

        // ── Content ───────────────────────────────────────────────────────────
        if (!isRunning) {
            // Bluetooth is off
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📡", fontSize = 48.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Bluetooth is off",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Make sure Bluetooth is enabled and permissions are granted.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { viewModel.refresh() },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                    ) {
                        Text("Start Scanning", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else if (users.isEmpty()) {
            // Running but no peers found yet
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = TealAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Scanning for nearby students...",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Make sure the other person's phone:\n• Has CampusLink open\n• Has Bluetooth ON\n• Is within ~10 metres",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(20.dp))
                    OutlinedButton(
                        onClick = { viewModel.refresh() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealAccent)
                    ) {
                        Text("Refresh Scan")
                    }
                }
            }
        } else {
            // Peers found — show list
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(users, key = { it.userId }) { user ->
                    UserCard(user = user) {
                        navController.navigate(Screen.Chat.createRoute(user.userId))
                    }
                }
            }
        }
    }
}

@Composable
fun NearbyStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
fun UserCard(user: com.campuslink.domain.model.User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(20)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator
        Box {
            Avatar(user.username, 52, true) // always true — they're connected
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(OnlineDot, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(Modifier.weight(1f)) {
            Text(user.username, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("@${user.userId}", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(OnlineDot, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "In range · ${user.zone.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
                    color = OnlineDot,
                    fontSize = 11.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            SignalBars(user.rssi)
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("Message", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
