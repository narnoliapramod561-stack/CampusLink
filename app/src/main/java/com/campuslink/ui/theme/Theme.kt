package com.campuslink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CampusLinkColorScheme = lightColorScheme(
    primary = Color(0xFF1B3A6B),          // Deep Navy
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF0F766E),         // Teal
    onSecondary = Color.White,
    surface = Color(0xFFF8FAFC),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
)

@Composable
fun CampusLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CampusLinkColorScheme,
        typography = Typography(),
        content = content
    )
}
