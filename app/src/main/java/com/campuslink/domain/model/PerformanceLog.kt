package com.campuslink.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "perf_logs")
data class PerformanceLog(
    @PrimaryKey val id: String,
    val messageId: String,
    val senderId: String,
    val receiverId: String,
    val hops: Int,
    val latencyMs: Long,
    val routePath: String,       // e.g. "A→B→C→D"
    val timestamp: Long,
    val success: Boolean
)
