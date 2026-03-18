package com.campuslink.domain.model

// In-memory route memory (not persisted — resets on app restart, by design)
data class RouteMemory(
    val receiverId: String,
    val bestNextHop: String,        // userId of best relay to reach receiverId
    val successCount: Int = 0,
    val failCount: Int = 0,
    val lastUpdated: Long = 0L
)
