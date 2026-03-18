package com.campuslink.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.campuslink.domain.model.ConversationPreview
import com.campuslink.domain.model.NetworkStats
import com.campuslink.domain.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val conversations   by viewModel.conversations.collectAsState()
    val nearbyUsers     by viewModel.nearbyUsers.collectAsState()
    val stats           by viewModel.networkStats.collectAsState()
    val isRunning       by viewModel.isBluetoothRunning.collectAsState()
    val myId            by viewModel.myUserId.collectAsState()
    val showDialog      by viewModel.showConnectDialog.collectAsState()
    val connectInput    by viewModel.connectInput.collectAsState()
    val connectError    by viewModel.connectError.collectAsState()
    val navigateToChat  by viewModel.navigateToChat.collectAsState()

    // Handle navigation triggered by manual connect
    LaunchedEffect(navigateToChat) {
        navigateToChat?.let { (userId, username) ->
            navController.navigate("chat/$userId/$username")
            viewModel.onNavigatedToChat()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    val otherNearby = nearbyUsers.filter { it.userId != myId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("CampusLink", fontWeight = FontWeight.Bold, color = Color(0xFFF5E6D8), fontSize = 18.sp)
                        Text("Offline mesh messaging", color = Color(0xFFA89B8F), fontSize = 11.sp)
                    }
                },
                actions = {
                    // Manual connect button in top bar
                    IconButton(onClick = viewModel::openConnectDialog) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color(0xFFF5E6D8))
                    }
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                if (isRunning) "● ON" else "● OFF",
                                fontSize = 11.sp,
                                color = Color(0xFFF5E6D8)
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isRunning) Color(0xFF16A34A).copy(alpha = 0.2f)
                                             else Color(0xFFDC2626).copy(alpha = 0.2f)
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0C0A))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openConnectDialog,
                containerColor = Color(0xFF2A1F18),
                contentColor = Color(0xFFF5E6D8),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF1A1410))
        ) {
            // Stats bar
            StatsBar(stats)

            // Tabs: Chats | Nearby
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0C0A)),
            ) {
                listOf("Chats (${conversations.size})", "Nearby (${otherNearby.size})").forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        modifier = Modifier.weight(1f),
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == i) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (selectedTab == i) Color(0xFFF5E6D8) else Color(0xFFA89B8F)
                            )
                        }
                    )
                }
            }
            Divider(color = Color(0xFF2A1F18))

            when (selectedTab) {
                0 -> ChatsTab(
                    conversations = conversations,
                    myId = myId,
                    onChatClick = { conv ->
                        navController.navigate("chat/${conv.partnerId}/${conv.partnerName}")
                    },
                    onNewChat = viewModel::openConnectDialog
                )
                1 -> NearbyTab(
                    users = otherNearby,
                    onChatClick = { user ->
                        navController.navigate("chat/${user.userId}/${user.username}")
                    },
                    onRefresh = viewModel::refreshScan
                )
            }
        }
    }

    // Manual connect dialog
    if (showDialog) {
        ConnectByIdDialog(
            input      = connectInput,
            error      = connectError,
            onChange   = viewModel::onConnectInputChange,
            onConfirm  = viewModel::confirmConnect,
            onDismiss  = viewModel::dismissConnectDialog
        )
    }
}

// ── Chats Tab (WhatsApp-style) ────────────────────────────────────────────────

@Composable
fun ChatsTab(
    conversations: List<ConversationPreview>,
    myId: String,
    onChatClick: (ConversationPreview) -> Unit,
    onNewChat: () -> Unit
) {
    if (conversations.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("💬", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No conversations yet",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color(0xFFF5E6D8)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Tap + to start a chat by User ID",
                fontSize = 13.sp,
                color = Color(0xFFA89B8F)
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextButton(
                onClick = onNewChat,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0x66E89A5B))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Chat", fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(conversations, key = { it.partnerId }) { conv ->
                ConversationRow(conv = conv, onClick = { onChatClick(conv) })
                Divider(
                    modifier = Modifier.padding(start = 76.dp),
                    color = Color(0xFF2A1F18),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
fun ConversationRow(conv: ConversationPreview, onClick: () -> Unit) {
    val avatarColors = listOf(
        Color(0xFF2A1F18), Color(0xFF1A1410), Color(0xFF3A2F28)
    )
    val avatarColor = avatarColors[conv.partnerId.hashCode().and(0x7FFFFFFF) % avatarColors.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = conv.partnerName.take(1).uppercase(),
                color = Color(0xFFF5E6D8),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conv.partnerName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFFF5E6D8)
                )
                Text(
                    text = formatTime(conv.lastTimestamp),
                    fontSize = 11.sp,
                    color = Color(0xFFA89B8F)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (conv.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                }
                Text(
                    text = conv.lastMessage,
                    fontSize = 13.sp,
                    color = Color(0xFFA89B8F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ── Nearby Tab ────────────────────────────────────────────────────────────────

@Composable
fun NearbyTab(users: List<User>, onChatClick: (User) -> Unit, onRefresh: () -> Unit) {
    if (users.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(color = Color(0x66E89A5B), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(14.dp))
            Text("Scanning for nearby students...", color = Color(0xFFA89B8F), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onRefresh,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0x66E89A5B))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Refresh")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(users, key = { it.userId }) { user ->
                NearbyUserRow(user = user, onClick = { onChatClick(user) })
                Divider(
                    modifier = Modifier.padding(start = 76.dp),
                    color = Color(0xFF2A1F18),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

@Composable
fun NearbyUserRow(user: User, onClick: () -> Unit) {
    val avatarColors = listOf(
        Color(0xFF2A1F18), Color(0xFF1A1410), Color(0xFF3A2F28)
    )
    val avatarColor = avatarColors[user.userId.hashCode().and(0x7FFFFFFF) % avatarColors.size]

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.username.take(1).uppercase(),
                color = Color(0xFFF5E6D8),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Online dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (user.isOnline) Color(0xFF16A34A) else Color(0xFF3A2F28))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(user.username, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFFF5E6D8))
            }
            Text(
                "@${user.userId}",
                fontSize = 12.sp,
                color = Color(0xFFA89B8F),
                modifier = Modifier.padding(start = 14.dp)
            )
        }

        TextButton(
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0x66E89A5B))
        ) {
            Text("Chat", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

// ── Stats bar ─────────────────────────────────────────────────────────────────

@Composable
fun StatsBar(stats: NetworkStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F0C0A))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatPill("${stats.activeNodes} nodes",    Color(0xFFA89B8F))
        StatPill("${stats.messagesSent} sent",    Color(0xFFA89B8F))
        StatPill("${stats.messagesRelayed} relayed", Color(0xFFA89B8F))
        StatPill("${stats.messagesDelivered} delivered", Color(0xFFA89B8F))
    }
}

@Composable
fun StatPill(label: String, color: Color) {
    Text(
        text = label,
        fontSize = 11.sp,
        color = color,
        fontWeight = FontWeight.Medium
    )
}

// ── Manual Connect Dialog ─────────────────────────────────────────────────────

@Composable
fun ConnectByIdDialog(
    input: String,
    error: String?,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "New Chat",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFFF5E6D8)
            )
        },
        text = {
            Column {
                Text(
                    "Enter the User ID of the person you want to message.\n" +
                    "They don't need to be nearby — the message will be delivered when a relay path becomes available.",
                    fontSize = 13.sp,
                    color = Color(0xFFA89B8F),
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = onChange,
                    label = { Text("User ID", color = Color(0xFFA89B8F)) },
                    placeholder = { Text("e.g. alice123", color = Color(0xFFA89B8F)) },
                    isError = error != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0x66E89A5B),
                        errorBorderColor   = Color(0xFFDC2626),
                        unfocusedBorderColor = Color(0xFF2A1F18),
                        focusedTextColor = Color(0xFFF5E6D8),
                        unfocusedTextColor = Color(0xFFF5E6D8)
                    )
                )
                if (error != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(error, color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0x66E89A5B))
            ) {
                Text("Open Chat", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA89B8F))
            }
        },
        containerColor = Color(0xFF1A1410),
        shape = RoundedCornerShape(20.dp)
    )
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "now"
        diff < 3_600_000L -> "${diff / 60_000}m"
        diff < 86_400_000L -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))
    }
}
