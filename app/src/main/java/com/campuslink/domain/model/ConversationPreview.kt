package com.campuslink.domain.model

data class ConversationPreview(
    val partnerId: String,
    val partnerName: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val isOnline: Boolean = false,
    val unreadCount: Int = 0
)
