package com.campuslink.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
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
            title = { Text("New Chat", color = TextPrimary) },
            text = {
                Column {
                    Text("Enter User ID — message will reach them when a relay path is available", 
                        color = TextSecondary, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = dialogInput, onValueChange = { viewModel.onDialogInput(it) },
                        placeholder = { Text("e.g. user_123", color = TextHint) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TealAccent,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    if (dialogError != null) {
                        Text(dialogError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmConnect() }) {
                    Text("Open Chat", color = TealAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialog() }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(Modifier.fillMaxSize().background(CL.BgDeep)) {
        HomeTopBar()
        
        Spacer(Modifier.height(16.dp))
        
        // Chat List
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item { 
                Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Messages", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    FloatingActionButton(onClick = { viewModel.openDialog() }, containerColor = TealAccent, contentColor = WarmBlack, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            }
            
            if (previews.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("💬", fontSize = 48.sp)
                            Text("No conversations yet", color = TextPrimary, fontSize = 16.sp)
                            Text("Tap + to connect by User ID", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(previews) { preview ->
                    ConversationRow(preview) { navController.navigate(Screen.Chat.createRoute(preview.partnerId)) }
                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                }
            }
        }
        
        // Connectivity Status Pill
        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
            Row(Modifier.glassCard(20).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(OnlineDot, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text("${stats.activeNodes} nodes in range", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun HomeTopBar() {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Lounge", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Text("Encrypted Mesh Protocol", color = TextSecondary, fontSize = 11.sp)
        }
        Box(Modifier.size(40.dp).glassCard(12), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Settings, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun ConversationRow(preview: com.campuslink.domain.model.ConversationPreview, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Avatar(preview.partnerName, 52, preview.isOnline)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(preview.partnerName, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(formatTime(preview.lastTimestamp), color = TextHint, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(preview.lastMessage, color = TextSecondary, fontSize = 14.sp, maxLines = 1)
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
