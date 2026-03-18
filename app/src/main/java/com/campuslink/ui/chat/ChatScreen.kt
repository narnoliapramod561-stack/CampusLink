package com.campuslink.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.campuslink.domain.model.MessagePriority
import com.campuslink.domain.model.MessageTargetType
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.*

@Composable
fun ChatScreen(userId: String, targetType: String = MessageTargetType.USER.name, onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val partner by viewModel.partner.collectAsState()
    val text by viewModel.text.collectAsState()
    val priority by viewModel.currentPriority.collectAsState()
    val listState = rememberLazyListState()

    var priorityExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(userId) { viewModel.load(userId) }
    LaunchedEffect(messages.size) { if(messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth().background(WarmBlack).padding(top = 16.dp, bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrimary) }
                    Avatar(partner?.username ?: "U", 40, partner?.isOnline ?: false)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(partner?.username ?: "User", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(if (partner?.isOnline == true) "Direct Reach" else "Relay Mode", color = TealAccent, fontSize = 11.sp)
                    }
                }
                HorizontalDivider(Modifier.padding(top = 8.dp), color = DividerColor)
            }
        },
        bottomBar = {
            // Unified Chat Input Pill
            Row(Modifier.fillMaxWidth().padding(16.dp).glassCard(28).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton(onClick = { priorityExpanded = true }) {
                        Text(if (priority == MessagePriority.NORMAL) "💬" else priority.emoji, fontSize = 20.sp)
                    }
                    DropdownMenu(
                        expanded = priorityExpanded,
                        onDismissRequest = { priorityExpanded = false },
                        modifier = Modifier.background(CL.BgDeep)
                    ) {
                        MessagePriority.values().forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.emoji} ${p.label}", color = TextPrimary) },
                                onClick = { viewModel.onPriorityChange(p); priorityExpanded = false }
                            )
                        }
                    }
                }

                val borderModifier = when (priority) {
                    MessagePriority.EMERGENCY -> Modifier.border(1.dp, Color.Red, RoundedCornerShape(24.dp))
                    MessagePriority.IMPORTANT -> Modifier.border(1.dp, AmberGlow, RoundedCornerShape(24.dp))
                    else -> Modifier
                }

                TextField(
                    value = text, onValueChange = { viewModel.onText(it) },
                    placeholder = { Text("Write a message...", color = TextHint, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f).then(borderModifier),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    )
                )

                IconButton(
                    onClick = { viewModel.send(targetType) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(40.dp).background(if(text.isNotBlank()) TealAccent else GlassWhite, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = if(text.isNotBlank()) WarmBlack else TextHint, modifier = Modifier.size(18.dp))
                }
            }
        },
        containerColor = WarmBlack
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), state = listState) {
            items(messages) { msg ->
                val isMe = msg.senderId != userId
                MessageBubble(msg, isMe)
            }
        }
    }
}

@Composable
fun MessageBubble(msg: com.campuslink.domain.model.Message, isMe: Boolean) {
    val bubbleColor = if (isMe) SentBubble else ReceivedBubble
    
    val bubbleModifier = when (msg.priority) {
        MessagePriority.EMERGENCY.name -> Modifier.border(2.dp, Color.Red, RoundedCornerShape(18.dp))
        MessagePriority.IMPORTANT.name -> Modifier.border(1.dp, AmberGlow, RoundedCornerShape(18.dp))
        else -> Modifier
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        if (msg.priority == MessagePriority.EMERGENCY.name) {
            Text("🚨 EMERGENCY", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
        } else if (msg.priority == MessagePriority.IMPORTANT.name) {
            Text("⚠️ IMPORTANT", color = AmberGlow, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
        }

        Column(Modifier.glassCard(18).then(bubbleModifier).background(bubbleColor).padding(12.dp)) {
            Text(msg.content, color = TextPrimary, fontSize = 15.sp)
            
            if (msg.targetType == MessageTargetType.ZONE.name && !isMe) {
                Text("Zone: ${msg.receiverId}", color = TextHint, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            } else if (msg.targetType == MessageTargetType.BROADCAST.name) {
                Text("📢 Campus Broadcast", color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End).padding(top = 4.dp)) {
                Text(com.campuslink.ui.home.formatTime(msg.timestamp), color = TextHint, fontSize = 10.sp)
                if (isMe) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.DoneAll, null, 
                        tint = if (msg.status == "DELIVERED") TealAccent else TextHint, 
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        if (msg.hopCount > 0) {
            val conf = if (msg.deliveryConfidence < 1.0f) " (${(msg.deliveryConfidence*100).toInt()}% conf)" else ""
            Text("${msg.hopCount} hops via mesh$conf", color = TextHint, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
