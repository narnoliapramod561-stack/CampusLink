package com.campuslink.ui.theme

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// All colors defined as top-level objects for use everywhere
object CL {
    val BgDeep        = Color(0xFF0D0B08)
    val BgSurface     = Color(0xFF1A1410)
    val BgCard        = Color(0xFF251E18)
    val Glass         = Color(0x14FFFFFF)
    val GlassBorder   = Color(0x1FFFFFFF)
    val Teal          = Color(0xFF00C896)
    val Blue          = Color(0xFF4A9EFF)
    val SentBubble    = Color(0xFF1E5A3A)
    val ReceivedBubble= Color(0xFF2A2420)
    val TextPrimary   = Color(0xFFF5F0EB)
    val TextSecondary = Color(0xFF8A8078)
    val TextHint      = Color(0xFF4A4440)
    val Online        = Color(0xFF00D46A)
    val Error         = Color(0xFFFF5050)
    val Amber         = Color(0xFFFFA040)
    val Delivered     = Color(0xFF00C896)
    val Divider       = Color(0x0FFFFFFF)
    val NavBg         = Color(0xFF161210)
}

// Keep the top-level vals for legacy compatibility if needed
val WarmBlack = CL.BgDeep
val DeepBrown = CL.BgSurface
val SurfaceBrown = CL.BgCard
val GlassWhite = CL.Glass
val GlassBorder = CL.GlassBorder
val AmberGlow = CL.Amber
val TealAccent = CL.Teal
val BlueAccent = CL.Blue
val TextPrimary = CL.TextPrimary
val TextSecondary = CL.TextSecondary
val TextHint = CL.TextHint
val SentBubble = CL.SentBubble
val ReceivedBubble = CL.ReceivedBubble
val OnlineDot = CL.Online
val MsgDelivered = CL.Delivered
val DividerColor = CL.Divider

// Reusable glass card modifier
fun Modifier.glassCard(cornerRadius: Dp = 20.dp): Modifier = this
    .background(
        brush = Brush.linearGradient(listOf(Color(0x1AFFFFFF), Color(0x08FFFFFF))),
        shape = RoundedCornerShape(cornerRadius)
    )
    .border(
        width = 0.5.dp,
        brush = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x0AFFFFFF))),
        shape = RoundedCornerShape(cornerRadius)
    )

private val DarkColorScheme = darkColorScheme(
    primary = TealAccent,
    onPrimary = WarmBlack,
    primaryContainer = SurfaceBrown,
    secondary = AmberGlow,
    onSecondary = WarmBlack,
    surface = SurfaceBrown,
    background = WarmBlack,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun CampusLinkTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = WarmBlack.toArgb()
            window.navigationBarColor = WarmBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(colorScheme = DarkColorScheme, content = content)
}
