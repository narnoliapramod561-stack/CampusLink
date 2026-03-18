package com.campuslink.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.navigation.NavHostController
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.*

@Composable
fun SettingsScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val username by viewModel.username.collectAsState()

    Column(Modifier.fillMaxSize().background(CL.BgDeep)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // SECTION 1 — Profile card
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().glassCard(20).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(username.takeIf { it.isNotBlank() } ?: "U", 72, true)
                    Spacer(Modifier.height(8.dp))
                    Text(username, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("@$userId", color = TextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { /* Not implemented */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, TealAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Edit Profile", color = TealAccent)
                    }
                }
            }

            item { SectionLabel("NETWORK") }
            item {
                Column(Modifier.glassCard(20).padding(4.dp)) {
                    GlassSettingsRow(
                        icon = Icons.Default.WifiTethering, 
                        title = "Auto-connect", 
                        subtitle = "Automatically connect to nearby peers",
                        trailing = {
                            Switch(
                                checked = settings.autoConnect, 
                                onCheckedChange = { viewModel.updateSettings(settings.copy(autoConnect = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = TealAccent, checkedTrackColor = TealAccent.copy(alpha = 0.5f))
                            )
                        }
                    )
                    // Hops Slider
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Max Hops", color = Color.White, fontSize = 15.sp)
                            Badge(containerColor = TealAccent.copy(0.15f)) { Text("${settings.maxHops}", color = TealAccent) }
                        }
                        Slider(value = settings.maxHops.toFloat(), onValueChange = { viewModel.updateSettings(settings.copy(maxHops = it.toInt())) },
                            valueRange = 1f..10f, colors = SliderDefaults.colors(thumbColor = TealAccent, activeTrackColor = TealAccent))
                    }
                }
            }

            item { SectionLabel("SECURITY") }
            item {
                Column(Modifier.glassCard(20).padding(4.dp)) {
                    GlassSettingsRow(
                        icon = Icons.Default.Lock, 
                        title = "Encryption", 
                        subtitle = "Coming in v2.0 — AES-256",
                        trailing = {
                            StatusBadge("PLANNED", false) // isSuccess = false for planned
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                Button(
                    onClick = { viewModel.logout { navController.navigate("auth") { popUpTo(0) { inclusive = true } } } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color.Red.copy(0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, null, tint = Color.Red.copy(0.8f))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", color = Color.Red.copy(0.8f), fontWeight = FontWeight.Bold)
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
