package com.campuslink.domain.model

enum class MessagePriority(val label: String, val emoji: String, val ttlOverride: Int?) {
    NORMAL    ("Normal",    "",   null),
    IMPORTANT ("Important", "⚠️",  9),
    EMERGENCY ("Emergency", "🚨", 15)  // bypasses normal TTL cap
}
