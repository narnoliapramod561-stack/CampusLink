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
import com.campuslink.domain.model.LpuZone
import com.campuslink.domain.model.UserRole
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.*

@Composable
fun SettingsScreen(navController: NavHostController, viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsState()
    val userId by viewModel.userId.collectAsState()
    val username by viewModel.username.collectAsState()
    val zone by viewModel.zone.collectAsState()
    val role by viewModel.role.collectAsState()
    val dept by viewModel.department.collectAsState()

    Column(Modifier.fillMaxSize().background(CL.BgDeep)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Settings & Profile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().glassCard(20).padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(username.takeIf { it.isNotBlank() } ?: "U", 72, true)
                    Spacer(Modifier.height(8.dp))
                    Text(username, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("@$userId", color = TextSecondary, fontSize = 13.sp)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    val parsedRole = try { UserRole.valueOf(role) } catch(e: Exception) { UserRole.STUDENT }
                    val parsedZone = LpuZone.fromName(zone)
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ProfileBadge("Role", "${parsedRole.emoji} ${parsedRole.displayName}")
                        ProfileBadge("Zone", "${parsedZone.emoji} ${parsedZone.displayName}")
                    }
                    if (dept.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(dept, color = TealAccent, fontSize = 13.sp)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { /* Not implemented for demo */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, TealAccent),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Edit Profile", color = TealAccent)
                    }
                }
            }

            item { SectionLabel("NETWORK PIPELINES") }
            item {
                Column(Modifier.glassCard(20).padding(4.dp)) {
                    GlassSettingsRow(
                        icon = Icons.Default.BatterySaver, 
                        title = "Battery Saver Mode", 
                        subtitle = "Reduce relay power consumption",
                        trailing = {
                            Switch(
                                checked = settings.batterySaverMode, 
                                onCheckedChange = { viewModel.updateSettings(settings.copy(batterySaverMode = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = TealAccent, checkedTrackColor = TealAccent.copy(alpha = 0.5f))
                            )
                        }
                    )
                    GlassSettingsRow(
                        icon = Icons.Default.Timeline, 
                        title = "Adaptive TTL", 
                        subtitle = "Dynamic routing based on peer density",
                        trailing = {
                            Switch(
                                checked = settings.adaptiveTtl, 
                                onCheckedChange = { viewModel.updateSettings(settings.copy(adaptiveTtl = it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = TealAccent, checkedTrackColor = TealAccent.copy(alpha = 0.5f))
                            )
                        }
                    )
                }
            }

            item { SectionLabel("SIMULATION") }
            item {
                Column(Modifier.glassCard(20).padding(4.dp)) {
                    GlassSettingsRow(
                        icon = Icons.Default.Science, 
                        title = "Dev: LPU Demo Mode", 
                        subtitle = "Inject 5 virtual LPU users into mesh",
                        trailing = {
                            Switch(
                                checked = settings.demoMode, 
                                onCheckedChange = { viewModel.toggleDemoMode(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = AmberGlow, checkedTrackColor = AmberGlow.copy(alpha = 0.5f))
                            )
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

@Composable
fun ProfileBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextHint, fontSize = 11.sp)
        Box(Modifier.background(WarmBlack.copy(0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
