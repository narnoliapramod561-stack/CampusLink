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
    val rssi: Int = 0            // signal strength — used for smart routing
)
