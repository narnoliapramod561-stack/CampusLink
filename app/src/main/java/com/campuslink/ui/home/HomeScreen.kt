package com.campuslink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
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
import com.campuslink.domain.model.LpuZone
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.*
import com.campuslink.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(navController: NavHostController, viewModel: HomeViewModel = hiltViewModel()) {
    val previews by viewModel.conversations.collectAsState()
    val stats by viewModel.networkStats.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val dialogInput by viewModel.dialogInput.collectAsState()
    val dialogError by viewModel.dialogError.collectAsState()
    val navigate by viewModel.navigate.collectAsState()
    
    // Quick Demo Mode check from Flow
    val settingsFlow = viewModel.settingsFlow.collectAsState(initial = null)

    LaunchedEffect(navigate) {
        navigate?.let {
            navController.navigate(Screen.Chat.createRoute(it.first))
            viewModel.onNavigated()
        }
    }
    
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialog() },
            containerColor = CL.BgCard,
            title = { Text("New Direct Chat", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter User ID to establish a mesh tunnel.", color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = dialogInput, onValueChange = { viewModel.onDialogInput(it) },
                        placeholder = { Text("e.g. user_123", color = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent, unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        )
                    )
                    if (dialogError != null) Text(dialogError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.confirmConnect() }) { Text("Connect", color = TealAccent, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { viewModel.dismissDialog() }) { Text("Cancel", color = TextSecondary) } }
        )
    }

    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().background(CL.BgDeep).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("CampusLink", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Decentralized Network Lounge", color = TextSecondary, fontSize = 11.sp)
                }
                Box(Modifier.size(40.dp).glassCard(12).clickable { navController.navigate("groups") }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Group, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
            }
        },
        containerColor = CL.BgDeep,
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openDialog() }, containerColor = TealAccent, contentColor = WarmBlack) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            
            if (settingsFlow.value?.demoMode == true) {
                Box(Modifier.fillMaxWidth().background(AmberGlow.copy(0.2f)).padding(8.dp), contentAlignment = Alignment.Center) {
                    Text("🧪 Demo Mode Active (Virtual nodes enabled)", color = AmberGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                if (previews.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💬", fontSize = 48.sp)
                                Text("No private lounges yet", color = TextPrimary, fontSize = 16.sp)
                                Text("Tap + to connect or use Campus Broadcast", color = TextSecondary, fontSize = 13.sp)
                            }
                        }
                    }
                } else {
                    items(previews, key = { it.partnerId }) { preview ->
                        ConversationRow(preview) { navController.navigate(Screen.Chat.createRoute(preview.partnerId)) }
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }
                }
            }
            
            // Stats Bar
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.glassCard(16).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).background(OnlineDot, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text("${stats.activeNodes} Nodes", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                
                if (stats.messagesPending > 0) {
                    Box(Modifier.background(Color.Red.copy(0.2f), RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text("${stats.messagesPending} Pending", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationRow(preview: com.campuslink.domain.model.ConversationPreview, onClick: () -> Unit) {
    val isEmergency = preview.hasEmergency
    val zoneObj = LpuZone.fromName(preview.partnerZone)
    
    val bg = if (isEmergency) Color.Red.copy(0.05f) else Color.Transparent
    val border = if (isEmergency) Modifier.border(1.dp, Color.Red.copy(0.3f), RoundedCornerShape(12.dp)) else Modifier
    
    Row(Modifier.fillMaxWidth().then(border).background(bg, RoundedCornerShape(12.dp)).clickable { onClick() }.padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(preview.partnerName, 50, preview.isOnline)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(preview.partnerName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (zoneObj.name != "UNKNOWN") {
                        Spacer(Modifier.width(4.dp))
                        Text(zoneObj.emoji, fontSize = 12.sp)
                    }
                }
                Text(formatTime(preview.lastTimestamp), color = if (isEmergency) Color.Red else TextHint, fontSize = 12.sp, fontWeight = if(isEmergency) FontWeight.Bold else FontWeight.Normal)
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isEmergency) {
                    Text("⚠️ ", fontSize = 12.sp)
                }
                Text(preview.lastMessage, color = if (isEmergency) TextPrimary else TextSecondary, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}

fun formatTime(ts: Long): String {
    val now = Calendar.getInstance()
    val msg = Calendar.getInstance().apply { timeInMillis = ts }
    return if (now.get(Calendar.DATE) == msg.get(Calendar.DATE)) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
    } else {
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts))
    }
}
