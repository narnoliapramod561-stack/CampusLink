package com.campuslink.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val messageId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    var ttl: Int = 7,
    var hopCount: Int = 0,
    val expiry: Long = System.currentTimeMillis() + 86_400_000L,
    val status: String = "SENDING"
)
