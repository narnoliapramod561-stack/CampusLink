package com.campuslink.domain.model

data class AppSettings(
    val autoConnect: Boolean = true,
    val maxHops: Int = 7,
    val retryLimit: Int = 3,
    val heartbeatInterval: Long = 10_000L,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showHopCount: Boolean = true,
    val encryptionEnabled: Boolean = false,
    val batterySaverMode: Boolean = false,
    val adaptiveTtl: Boolean = true,
    val currentZone: String = "BLOCK_32",
    val currentRole: String = "STUDENT",
    val department: String = ""
    // FIX: demoMode field removed.
    // It caused fake LPU users to be inserted into Room DB and appear in the
    // Chats list. It also intercepted real Bluetooth sends in ChatViewModel.
)
