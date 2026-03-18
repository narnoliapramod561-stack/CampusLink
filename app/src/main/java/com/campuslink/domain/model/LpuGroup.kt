package com.campuslink.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "groups")
data class LpuGroup(
    @PrimaryKey val groupId: String,
    val name: String,               // e.g. "#cse-2024", "Study Group 7"
    val zone: String = "",          // if zone-linked
    val createdBy: String = "",
    val memberIds: String = "",     // JSON array of userIds
    val createdAt: Long = System.currentTimeMillis(),
    val isChannel: Boolean = false  // true = broadcast channel, false = group chat
)
