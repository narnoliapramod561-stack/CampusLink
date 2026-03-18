package com.campuslink.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.campuslink.domain.model.LpuGroup
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.*
import com.google.gson.Gson

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(onOpenChat: (String) -> Unit, viewModel: GroupViewModel = hiltViewModel()) {
    val groups by viewModel.groups.collectAsState()
    val myId by viewModel.myId.collectAsState()
    val showDialog by viewModel.showCreateDialog.collectAsState()
    val groupName by viewModel.groupName.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups & Channels", color = TextPrimary, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.openCreate() }) {
                        Icon(Icons.Default.Add, null, tint = TealAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CL.BgDeep)
            )
        },
        containerColor = CL.BgDeep
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("#️⃣ No groups yet. Create one to start messaging.", color = TextHint, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                    items(groups) { group ->
                        GroupCard(group, myId, onClick = {
                            viewModel.joinGroup(group.groupId)
                            onOpenChat(group.groupId)
                        })
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissCreate() },
            containerColor = CL.BgDeep,
            title = { Text("Create Group", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                TextField(
                    value = groupName,
                    onValueChange = { viewModel.onGroupName(it) },
                    placeholder = { Text("Group name (# added automatically)", color = TextHint) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = TealAccent, unfocusedIndicatorColor = GlassBorder,
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.createGroup() }) { Text("Create", color = TealAccent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissCreate() }) { Text("Cancel", color = TextHint) }
            }
        )
    }
}

@Composable
fun GroupCard(group: LpuGroup, myId: String, onClick: () -> Unit) {
    val members = try { Gson().fromJson(group.memberIds, Array<String>::class.java).toList() } catch (_: Exception) { emptyList() }
    val memberCount = members.size
    val isCreator = group.createdBy == myId

    Row(Modifier.fillMaxWidth().glassCard(16).clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(TealAccent.copy(alpha=0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Text("#", color = TealAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(group.name, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (isCreator) {
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.background(TealAccent.copy(alpha=0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Text("You", color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text("$memberCount members", color = TextHint, fontSize = 13.sp)
        }
        Text("Open", color = TealAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
