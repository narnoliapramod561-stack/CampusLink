package com.campuslink.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.campuslink.domain.model.LpuZone
import com.campuslink.domain.model.UserRole
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.glassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: AuthViewModel = hiltViewModel(), onLoginSuccess: () -> Unit) {
    val state by viewModel.isLoggedIn.collectAsState()
    var username by remember { mutableStateOf("") }
    val zone by viewModel.zone.collectAsState()
    val role by viewModel.role.collectAsState()
    val dept by viewModel.department.collectAsState()
    
    var zoneExpanded by remember { mutableStateOf(false) }
    var roleExpanded by remember { mutableStateOf(false) }
    
    LaunchedEffect(state) { if (state) onLoginSuccess() }

    Box(Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(WarmBlack, DeepBrown))
    ), contentAlignment = Alignment.Center) {
        
        // Background Glows
        Box(Modifier.size(300.dp).offset(x = (-100).dp, y = (-200).dp).background(AmberGlow.copy(0.05f), RoundedCornerShape(150.dp)))
        Box(Modifier.size(400.dp).offset(x = 150.dp, y = 200.dp).background(TealAccent.copy(0.05f), RoundedCornerShape(200.dp)))

        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("CampusLink", color = TextPrimary, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Decentralized Offline Mesh", color = TextSecondary, fontSize = 14.sp)
            
            Spacer(Modifier.height(32.dp))
            
            Column(Modifier.fillMaxWidth().glassCard(24).padding(24.dp)) {
                Text("Join Network", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                TextField(
                    value = username, onValueChange = { username = it },
                    placeholder = { Text("Display Name", color = TextHint) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = TealAccent,
                        unfocusedIndicatorColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                
                Spacer(Modifier.height(8.dp))
                
                TextField(
                    value = dept, onValueChange = { viewModel.onDepartment(it) },
                    placeholder = { Text("e.g. B.Tech CSE 2024", color = TextHint) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = TealAccent,
                        unfocusedIndicatorColor = GlassBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = zoneExpanded,
                    onExpandedChange = { zoneExpanded = !zoneExpanded }
                ) {
                    val currentZone = LpuZone.fromName(zone)
                    TextField(
                        value = "${currentZone.emoji} ${currentZone.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Your Campus Zone", color = TextHint) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = zoneExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = TealAccent, unfocusedIndicatorColor = GlassBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = zoneExpanded,
                        onDismissRequest = { zoneExpanded = false },
                        modifier = Modifier.background(CL.BgDeep)
                    ) {
                        LpuZone.values().forEach { z ->
                            DropdownMenuItem(
                                text = { Text("${z.emoji} ${z.displayName}", color = TextPrimary) },
                                onClick = { viewModel.onZone(z.name); zoneExpanded = false }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = roleExpanded,
                    onExpandedChange = { roleExpanded = !roleExpanded }
                ) {
                    val currentRole = UserRole.valueOf(role)
                    TextField(
                        value = "${currentRole.emoji} ${currentRole.displayName}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Your Role", color = TextHint) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(
                            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = TealAccent, unfocusedIndicatorColor = GlassBorder,
                            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleExpanded,
                        onDismissRequest = { roleExpanded = false },
                        modifier = Modifier.background(CL.BgDeep)
                    ) {
                        UserRole.values().forEach { r ->
                            DropdownMenuItem(
                                text = { Text("${r.emoji} ${r.displayName}", color = TextPrimary) },
                                onClick = { viewModel.onRole(r.name); roleExpanded = false }
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { if(username.isNotBlank()) viewModel.login(username) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Text("Enter Lounge", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(Modifier.height(16.dp))
                Text("LPU Campus Mesh Network", color = TextHint, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text("Lovely Professional University — Phagwara", color = TextHint, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
