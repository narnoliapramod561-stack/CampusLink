package com.campuslink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campuslink.ui.theme.*

fun Modifier.glassCard(radius: Int = 16) = this
    .clip(RoundedCornerShape(radius.dp))
    .background(GlassWhite)
    .border(1.dp, GlassBorder, RoundedCornerShape(radius.dp))

@Composable
fun Avatar(name: String, size: Int = 40, isOnline: Boolean = false) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier.size(size.dp).clip(CircleShape).background(SurfaceBrown),
            contentAlignment = Alignment.Center
        ) {
            Text(name.take(1).uppercase(), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = (size/2.5).sp)
        }
        if (isOnline) {
            Box(Modifier.size((size/4).dp).clip(CircleShape).background(WarmBlack).padding(2.dp)) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(OnlineDot))
            }
        }
    }
}

@Composable
fun SignalBars(rssi: Int) {
    val count = when { rssi > -60 -> 4; rssi > -70 -> 3; rssi > -80 -> 2; rssi > -90 -> 1; else -> 0 }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(4) { i ->
            Box(Modifier.width(3.dp).height((4 + (i * 3)).dp).clip(RoundedCornerShape(1.dp))
                .background(if (i < count) TealAccent else TextHint))
        }
    }
}

@Composable
fun GlassSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit = {},
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).glassCard(12), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                if (subtitle != null) Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            if (trailing != null) {
                trailing()
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = TextHint)
            }
        }
    }
}

@Composable
fun SectionLabel(label: String) {
    Text(label.uppercase(), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
}

@Composable
fun StatusBadge(text: String, isSuccess: Boolean) {
    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(if (isSuccess) TealAccent.copy(0.1f) else BlueAccent.copy(0.1f))
        .padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(text, color = if (isSuccess) TealAccent else BlueAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}
