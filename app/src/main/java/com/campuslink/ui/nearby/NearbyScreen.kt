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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.*
import com.campuslink.ui.navigation.Screen

@Composable
fun NearbyScreen(navController: NavHostController, viewModel: NearbyViewModel = hiltViewModel()) {
    val users by viewModel.users.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    Column(Modifier.fillMaxSize().background(CL.BgDeep)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Nearby", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            StatusBadge(if (isRunning) "SCANNING" else "OFFLINE", isRunning)
        }

        // Stats Strip
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).glassCard(16).padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            NearbyStatItem("${stats.activeNodes}", "Nodes", TealAccent)
            NearbyStatItem("${stats.messagesRelayed}", "Relays", AmberGlow)
            NearbyStatItem("${String.format("%.1f", stats.avgHopCount)}", "Hops", BlueAccent)
            NearbyStatItem("${(stats.relaySuccessRate * 100).toInt()}%", "Health", OnlineDot)
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("STUDENTS IN RANGE")

        if (users.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = TealAccent, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Scanning for students nearby...", color = TextSecondary, fontSize = 14.sp)
                    Button(onClick = { viewModel.refresh() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                        Text("Refresh", color = TealAccent)
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(users) { user ->
                    UserCard(user) { navController.navigate(Screen.Chat.createRoute(user.userId)) }
                }
                item { Spacer(Modifier.height(32.dp)) }
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
    Row(Modifier.fillMaxWidth().glassCard(20).clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(user.username, 52, user.isOnline)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(user.username, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("@${user.userId}", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(if(user.isOnline) OnlineDot else TextHint, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(if(user.isOnline) "In range" else "Relay mode", color = if(user.isOnline) OnlineDot else TextHint, fontSize = 11.sp)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            SignalBars(user.rssi)
            TextButton(onClick = onClick) {
                Text("Chat", color = TealAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}
