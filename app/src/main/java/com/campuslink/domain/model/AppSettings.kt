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
    // NEW v3:
    val batterySaverMode: Boolean = false,  // reduces scan freq, lowers relay rate
    val adaptiveTtl: Boolean = true,        // auto-adjust TTL based on network density
    val demoMode: Boolean = false,          // simulate 5 virtual LPU devices
    val currentZone: String = "BLOCK_32",
    val currentRole: String = "STUDENT",
    val department: String = ""
)
