package com.campuslink.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val messageId: String,
    val senderId: String,
    val receiverId: String,     // userId for USER type, zone name for ZONE, "ALL" for BROADCAST, groupId for GROUP
    val content: String,
    val timestamp: Long,
    var ttl: Int = 7,
    var hopCount: Int = 0,
    val expiry: Long = System.currentTimeMillis() + 86_400_000L,
    val status: String = "SENDING",
    // NEW FIELDS v3:
    val targetType: String = "USER",        // USER | ZONE | BROADCAST | GROUP
    val priority: String = "NORMAL",        // NORMAL | IMPORTANT | EMERGENCY
    val senderZone: String = "",            // zone of the sender at send time
    val senderRole: String = "STUDENT",     // role of sender
    val groupId: String = "",               // for GROUP type messages
    val pathHistory: String = "",           // JSON array of userId hops e.g. ["A","B","C"]
    val deliveryConfidence: Float = 1.0f   // 0.0–1.0 estimated delivery probability
)
