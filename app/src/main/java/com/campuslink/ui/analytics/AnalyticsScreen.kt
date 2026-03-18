package com.campuslink.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.campuslink.domain.model.LpuZone
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(navController: NavHostController, viewModel: AnalyticsViewModel = hiltViewModel()) {
    val stats by viewModel.stats.collectAsState()
    val logs by viewModel.logs.collectAsState()


    Column(Modifier.fillMaxSize().background(CL.BgDeep)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Network Intelligence", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { viewModel.clearLogs() }) { Icon(Icons.Default.DeleteSweep, null, tint = TextSecondary) }
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item { SectionLabel("LPU ZONE ACTIVITY") }
            item {
                Row(Modifier.fillMaxWidth().glassCard(20).padding(16.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Active Zones", color = TextSecondary, fontSize = 12.sp)
                        Text("${LpuZone.values().size}", color = TealAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Online Devices", color = TextSecondary, fontSize = 12.sp)
                        Text("${stats.activeNodes}", color = AmberGlow, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            item { Spacer(Modifier.height(16.dp)) }
            item { SectionLabel("ROUTING ENGINE") }
            item {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard("Sent", "${stats.messagesSent}", Icons.Default.Chat, TealAccent, Modifier.weight(1f))
                        MetricCard("Relayed", "${stats.messagesRelayed}", Icons.Default.AltRoute, AmberGlow, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MetricCard("Pending", "${stats.messagesPending}", Icons.Default.HourglassEmpty, Color.Red.copy(0.7f), Modifier.weight(1f))
                        MetricCard("Success", "${(stats.relaySuccessRate * 100).toInt()}%", Icons.Default.CheckCircle, OnlineDot, Modifier.weight(1f))
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                Column(Modifier.fillMaxWidth().glassCard(20).padding(16.dp)) {
                    Text("Store & Forward Health", color = Color.White, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { stats.relaySuccessRate },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = TealAccent,
                        trackColor = GlassWhite,
                        strokeCap = StrokeCap.Round
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Average hop count: ${String.format("%.1f", stats.avgHopCount)}", color = TextSecondary, fontSize = 12.sp)
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item { SectionLabel("PENDING QUEUE") }
            item {
                if (stats.messagesPending == 0) {
                    Text("All messages delivered.", color = TextHint, modifier = Modifier.padding(16.dp))
                } else {
                    Row(Modifier.fillMaxWidth().glassCard(12).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, null, tint = AmberGlow)
                        Spacer(Modifier.width(16.dp))
                        Text("${stats.messagesPending} messages waiting for route", color = TextPrimary)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item { SectionLabel("RECENT SYSTEM EVENTS") }
            if (logs.isEmpty()) {
                item { Text("No performance logs yet.", color = TextHint, modifier = Modifier.padding(16.dp)) }
            } else {
                items(logs.take(10)) { log ->
                    LogItem(log)
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Column(modifier.glassCard(20).padding(16.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(12.dp))
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(title, color = TextSecondary, fontSize = 12.sp)
    }
}

@Composable
fun LogItem(log: com.campuslink.domain.model.PerformanceLog) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(if(!log.success) Color.Red else TealAccent, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Relay: ${log.routePath}", color = Color.White, fontSize = 14.sp)
            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
            Text("${log.hops} hops • ${log.latencyMs}ms • $time", color = TextSecondary, fontSize = 11.sp)
        }
    }
}
