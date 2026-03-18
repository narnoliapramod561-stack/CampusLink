package com.campuslink.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessageStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    userId: String,
    username: String,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val myId by viewModel.myUserId.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val activeNodes by viewModel.activeNodes.collectAsState()
    val connectionPath by viewModel.connectionPath.collectAsState()
    val listState = rememberLazyListState()

    // Dynamic Glow for premium UI
    val infinite = rememberInfiniteTransition()
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.1f, 
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    fun Modifier.glassmorphism() = this
        .background(Color(0x22F5E6D8), shape = RoundedCornerShape(24.dp))
        .border(1.dp, Color(0x11FFFFFF), RoundedCornerShape(24.dp))
        .blur(20.dp)

    LaunchedEffect(userId) {
        viewModel.initConversation(userId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F766E)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(username.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(username, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Text("@$userId", fontSize = 11.sp, color = Color(0xFFB0C4DE))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C0A)),
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1410),
                            Color(0xFF0F0C0A)
                        )
                    )
                )
        ) {
            // MAIN WARM LIGHT (cinematic glow)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x66E89A5B).copy(alpha = glowAlpha), // amber glow
                                Color.Transparent
                            ),
                            center = Offset(700f, 500f),
                            radius = 900f
                        )
                    )
            )

            // SECONDARY LIGHT (soft depth)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x33C97A3A),
                                Color.Transparent
                            ),
                            center = Offset(200f, 1000f),
                            radius = 1000f
                        )
                    )
            )

            Column(modifier = Modifier.fillMaxSize()) {
                // Connection Status Bar
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).glassmorphism()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isConnected) connectionPath else "Reconnecting...",
                            color = if (isConnected) Color(0xFF34D399) else Color(0xFFF87171),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Network: $activeNodes devices",
                            color = Color(0xFFA89B8F),
                            fontSize = 12.sp
                        )
                    }
                }

                LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.messageId }) { message ->
                    MessageBubble(message = message, isMyMessage = message.senderId == myId)
                }
            }

            // Input row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color.Black.copy(0.4f),
                        spotColor = Color.Black.copy(0.4f)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphism()
                        .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextField(
                        value = messageText,
                        onValueChange = viewModel::onMessageTextChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type here...", color = Color(0xFFA89B8F), fontSize = 15.sp) },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color(0xFFF5E6D8),
                            unfocusedTextColor = Color(0xFFF5E6D8),
                            cursorColor = Color(0x66E89A5B)
                        ),
                        maxLines = 4
                    )
                    
                    Icon(
                        imageVector = Icons.Default.Face, // Or any suitable icon
                        contentDescription = "Emoji/Attach",
                        tint = Color(0xFFA89B8F),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(24.dp)
                    )

                    IconButton(
                        onClick = viewModel::sendMessage,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1410)) // Dark circle
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color(0xFFF5E6D8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
}


@Composable
fun MessageBubble(message: Message, isMyMessage: Boolean) {
    val simulatedLatency = if (message.status != MessageStatus.SENDING.name && message.status != MessageStatus.PENDING.name) {
        (message.hopCount * 32) + 15 // Mock calculation based on hops
    } else null

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMyMessage) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isMyMessage) 16.dp else 4.dp,
                        bottomEnd = if (isMyMessage) 4.dp else 16.dp
                    ),
                    ambientColor = Color.Black.copy(0.3f),
                    spotColor = Color.Black.copy(0.3f)
                )
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp, topEnd = 16.dp,
                        bottomStart = if (isMyMessage) 16.dp else 4.dp,
                        bottomEnd = if (isMyMessage) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isMyMessage) Color(0xFF2A1F18) else Color(0xFF1A1410)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                color = Color(0xFFF5E6D8),
                fontSize = 15.sp
            )
        }

        if (isMyMessage) {
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (simulatedLatency != null) {
                    Text(
                        text = "Latency: ${simulatedLatency}ms • ",
                        color = Color(0xFFA89B8F),
                        fontSize = 10.sp
                    )
                }
                
                val statusText = when (message.status) {
                    MessageStatus.SENDING.name -> "Sending..."
                    MessageStatus.RELAYED.name -> "Relayed (${message.hopCount} hops)"
                    MessageStatus.DELIVERED.name -> "✓ Delivered"
                    MessageStatus.PENDING.name -> "Pending"
                    MessageStatus.FAILED.name -> "Failed"
                    else -> ""
                }
                val statusColor = when (message.status) {
                    MessageStatus.SENDING.name -> Color(0xFFA89B8F)
                    MessageStatus.RELAYED.name -> Color(0x66E89A5B)
                    MessageStatus.DELIVERED.name -> Color(0xFF34D399)
                    MessageStatus.PENDING.name -> Color(0xFFF97316)
                    else -> Color(0xFFA89B8F)
                }
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
    }
}
