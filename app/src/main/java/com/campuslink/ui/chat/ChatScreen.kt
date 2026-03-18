package com.campuslink.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.*

@Composable
fun ChatScreen(userId: String, onBack: () -> Unit, viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val partner by viewModel.partner.collectAsState()
    val text by viewModel.text.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(userId) { viewModel.load(userId) }
    LaunchedEffect(messages.size) { if(messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }

    Scaffold(
        topBar = {
            Column(Modifier.fillMaxWidth().background(WarmBlack).padding(top = 16.dp, bottom = 8.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = TextPrimary) }
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
                IconButton(onClick = { /* attachments */ }) { Icon(Icons.Default.Add, null, tint = TextSecondary) }
                TextField(
                    value = text, onValueChange = { viewModel.onText(it) },
                    placeholder = { Text("Write a message...", color = TextHint, fontSize = 14.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                IconButton(
                    onClick = { viewModel.send() },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.size(40.dp).background(if(text.isNotBlank()) TealAccent else GlassWhite, CircleShape)
                ) {
                    Icon(Icons.Default.Send, null, tint = if(text.isNotBlank()) WarmBlack else TextHint, modifier = Modifier.size(18.dp))
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
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
        Column(Modifier.glassCard(18).background(if (isMe) SentBubble else ReceivedBubble).padding(12.dp)) {
            Text(msg.content, color = TextPrimary, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.End).padding(top = 4.dp)) {
                Text(com.campuslink.ui.home.formatTime(msg.timestamp), color = TextHint, fontSize = 10.sp)
                if (isMe) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.DoneAll, null, tint = if(msg.status=="DELIVERED") TealAccent else TextHint, modifier = Modifier.size(14.dp))
                }
            }
        }
        if (msg.hopCount > 0) {
            Text("${msg.hopCount} hops via mesh", color = TextHint, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
