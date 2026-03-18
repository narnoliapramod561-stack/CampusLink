package com.campuslink.domain.model

data class AppSettings(
    val autoConnect: Boolean = true,
    val maxHops: Int = 7,
    val retryLimit: Int = 3,
    val heartbeatInterval: Long = 10_000L,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val showHopCount: Boolean = true,
    val encryptionEnabled: Boolean = false   // placeholder for v2
)
