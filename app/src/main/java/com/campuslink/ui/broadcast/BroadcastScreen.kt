package com.campuslink.ui.broadcast

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.campuslink.domain.model.LpuZone
import com.campuslink.domain.model.MessagePriority
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(viewModel: BroadcastViewModel = hiltViewModel()) {
    val tab by viewModel.tab.collectAsState()
    val zoneMessages by viewModel.zoneMessages.collectAsState()
    val broadcastMessages by viewModel.broadcastMessages.collectAsState()
    val usersPerZone by viewModel.usersPerZone.collectAsState()
    val content by viewModel.content.collectAsState()
    val selectedZone by viewModel.selectedZone.collectAsState()
    val priority by viewModel.priority.collectAsState()
    val myRole by viewModel.myRole.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Campus Broadcast", color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CL.BgDeep)
            )
        },
        containerColor = CL.BgDeep
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = CL.BgDeep,
                contentColor = TextPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.PrimaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tab]),
                        color = TealAccent
                    )
                }
            ) {
                Tab(selected = tab == 0, onClick = { viewModel.onTab(0) }, text = { Text("Zone Messages") })
                Tab(selected = tab == 1, onClick = { viewModel.onTab(1) }, text = { Text("Campus Broadcast") })
            }

            if (tab == 0) {
                ZoneTabContent(viewModel, zoneMessages, usersPerZone, content, selectedZone, priority)
            } else {
                BroadcastTabContent(viewModel, broadcastMessages, usersPerZone, content, priority, myRole)
            }
        }
    }
}

@Composable
fun ZoneTabContent(
    viewModel: BroadcastViewModel,
    messages: List<com.campuslink.domain.model.Message>,
    usersPerZone: Map<String, Int>,
    content: String,
    selectedZone: LpuZone?,
    priority: MessagePriority
) {
    Column(Modifier.fillMaxSize()) {
        LazyRow(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LpuZone.values()) { zone ->
                val isSelected = selectedZone == zone
                val bg = if (isSelected) TealAccent else GlassWhite
                val textColor = if (isSelected) Color.White else TextPrimary
                Box(
                    Modifier.glassCard(16).background(bg).clickable { viewModel.onZone(zone) }.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("${zone.emoji} ${zone.displayName.take(12)}...", color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        selectedZone?.let {
            val onlineCount = usersPerZone[it.name] ?: 0
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).glassCard(12).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("👥", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Text("$onlineCount", color = TealAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("students online in ${it.displayName}", color = TextHint, fontSize = 14.sp)
            }
        }

        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg -> MessageCard(msg) }
        }

        InputBar(
            content = content,
            onContentChange = { viewModel.onContent(it) },
            priority = priority,
            onPriorityChange = { viewModel.onPriority(it) },
            onSend = { viewModel.sendToZone() },
            placeholder = "Message to ${selectedZone?.displayName}..."
        )
    }
}

@Composable
fun BroadcastTabContent(
    viewModel: BroadcastViewModel,
    messages: List<com.campuslink.domain.model.Message>,
    usersPerZone: Map<String, Int>,
    content: String,
    priority: MessagePriority,
    myRole: String
) {
    val canBroadcast = viewModel.canBroadcast()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (!canBroadcast) {
            Column(Modifier.fillMaxWidth().glassCard(16).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Campaign, null, tint = TealAccent, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Broadcast Access Required", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Only Faculty and Admin can send campus-wide broadcasts. You can receive and relay broadcasts.", color = TextHint, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        } else {
            Column(Modifier.fillMaxWidth().glassCard(16).padding(16.dp)) {
                Text("📢 Campus-Wide Broadcast", color = TealAccent, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MessagePriority.values().forEach { p ->
                        val isSelected = priority == p
                        val bg = if (isSelected) TealAccent else Color.Transparent
                        val border = if (isSelected) Color.Transparent else GlassBorder
                        Box(Modifier.border(1.dp, border, RoundedCornerShape(12.dp)).background(bg, RoundedCornerShape(12.dp)).clickable { viewModel.onPriority(p) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("${p.emoji} ${p.label}", color = if(isSelected) Color.White else TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
                
                if (priority == MessagePriority.EMERGENCY) {
                    Text("⚠️ Will bypass TTL limits", color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
                
                TextField(
                    value = content, onValueChange = { viewModel.onContent(it) },
                    placeholder = { Text("Enter broadcast message...", color = TextHint) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = TealAccent, unfocusedIndicatorColor = GlassBorder,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )

                Button(
                    onClick = { viewModel.sendBroadcast() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    enabled = content.isNotBlank()
                ) {
                    Text("Send to All Campus Devices", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(150.dp)) {
                items(LpuZone.values()) { zone ->
                    val count = usersPerZone[zone.name] ?: 0
                    Row(Modifier.glassCard(8).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(zone.emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(zone.name.take(10), color = TextHint, fontSize = 10.sp)
                            Text("$count online", color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Received Broadcasts", color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg -> MessageCard(msg) }
        }
    }
}

@Composable
fun MessageCard(msg: com.campuslink.domain.model.Message) {
    val borderColor = when (msg.priority) {
        MessagePriority.EMERGENCY.name -> Color.Red
        MessagePriority.IMPORTANT.name -> TealAccent
        else -> Color.Transparent
    }
    
    val badge = when (msg.senderRole) {
        "ADMIN" -> "🔑"
        "FACULTY" -> "👨‍🏫"
        else -> "🎓"
    }

    val zoneObj = LpuZone.fromName(msg.senderZone)

    Column(Modifier.fillMaxWidth().glassCard(16).border(1.dp, borderColor, RoundedCornerShape(16.dp)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(badge, fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(msg.senderId, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.width(8.dp))
                Text(zoneObj.emoji, fontSize = 14.sp)
            }
            if (msg.priority != MessagePriority.NORMAL.name) {
                Box(Modifier.background(borderColor, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Text(msg.priority, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(msg.content, color = TextPrimary, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        Text(com.campuslink.ui.home.formatTime(msg.timestamp), color = TextHint, fontSize = 11.sp, modifier = Modifier.align(Alignment.End))
    }
}

@Composable
fun InputBar(
    content: String,
    onContentChange: (String) -> Unit,
    priority: MessagePriority,
    onPriorityChange: (MessagePriority) -> Unit,
    onSend: () -> Unit,
    placeholder: String
) {
    var priorityExpanded by remember { mutableStateOf(false) }

    val borderModifier = when (priority) {
        MessagePriority.EMERGENCY -> Modifier.border(1.dp, Color.Red, RoundedCornerShape(24.dp))
        MessagePriority.IMPORTANT -> Modifier.border(1.dp, AmberGlow, RoundedCornerShape(24.dp))
        else -> Modifier
    }

    Row(Modifier.fillMaxWidth().padding(16.dp).glassCard(28).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box {
            IconButton(onClick = { priorityExpanded = true }) {
                Text(if (priority == MessagePriority.NORMAL) "💬" else priority.emoji, fontSize = 20.sp)
            }
            DropdownMenu(expanded = priorityExpanded, onDismissRequest = { priorityExpanded = false }, modifier = Modifier.background(CL.BgDeep)) {
                MessagePriority.values().forEach { p ->
                    DropdownMenuItem(text = { Text("${p.emoji} ${p.label}", color = TextPrimary) }, onClick = { onPriorityChange(p); priorityExpanded = false })
                }
            }
        }
        TextField(
            value = content, onValueChange = onContentChange,
            placeholder = { Text(placeholder, color = TextHint, fontSize = 14.sp) },
            modifier = Modifier.weight(1f).then(borderModifier),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
            )
        )
        IconButton(
            onClick = onSend,
            enabled = content.isNotBlank(),
            modifier = Modifier.size(40.dp).background(if(content.isNotBlank()) TealAccent else GlassWhite, CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, null, tint = if(content.isNotBlank()) WarmBlack else TextHint, modifier = Modifier.size(18.dp))
        }
    }
}
