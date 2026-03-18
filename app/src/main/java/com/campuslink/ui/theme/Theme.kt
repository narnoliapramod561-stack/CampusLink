package com.campuslink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CampusLinkColorScheme = lightColorScheme(
    primary = Color(0xFF0F0C0A),          // Deep Brown/Black
    onPrimary = Color(0xFFF5E6D8),
    primaryContainer = Color(0xFF1A1410),
    secondary = Color(0x66E89A5B),         // Soft Amber
    onSecondary = Color(0xFFF5E6D8),
    surface = Color(0xFF1A1410),
    background = Color(0xFF1A1410),
    onBackground = Color(0xFFF5E6D8),
    onSurface = Color(0xFFF5E6D8),
)

@Composable
fun CampusLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CampusLinkColorScheme,
        typography = Typography(),
        content = content
    )
}
