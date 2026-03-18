package com.campuslink.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String,
    val username: String,
    val deviceAddress: String,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val rssi: Int = 0,
    // NEW v3:
    val zone: String = "BLOCK_32",          // LpuZone.name
    val role: String = "STUDENT",           // UserRole.name
    val reliabilityScore: Float = 1.0f,     // 0.0–1.0, updated after each relay
    val messagesRelayed: Int = 0,           // lifetime relay count
    val department: String = ""            // e.g. "B.Tech CSE"
)
