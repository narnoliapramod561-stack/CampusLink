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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.campuslink.ui.theme.*
import com.campuslink.ui.components.glassCard

@Composable
fun AuthScreen(viewModel: AuthViewModel = hiltViewModel(), onLoginSuccess: () -> Unit) {
    val state by viewModel.isLoggedIn.collectAsState()
    var username by remember { mutableStateOf("") }
    
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
            
            Spacer(Modifier.height(48.dp))
            
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
                
                Spacer(Modifier.height(24.dp))
                
                Button(
                    onClick = { if(username.isNotBlank()) viewModel.login(username) },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Text("Enter Lounge", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
