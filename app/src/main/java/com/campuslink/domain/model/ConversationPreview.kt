package com.campuslink.domain.model

/**
 * Represents one row in the WhatsApp-style conversations list.
 * Built from the most recent Message in each conversation.
 */
data class ConversationPreview(
    val partnerId: String,       // the other user's ID
    val partnerName: String,     // their display name (from User table, or partnerId if unknown)
    val lastMessage: String,     // content of most recent message
    val lastTimestamp: Long,     // for sorting
    val unreadCount: Int = 0,    // future use
    val isOnline: Boolean = false
)
