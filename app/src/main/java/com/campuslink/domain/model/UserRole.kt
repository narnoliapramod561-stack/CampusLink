package com.campuslink.domain.model

enum class UserRole(val displayName: String, val emoji: String) {
    STUDENT ("Student",  "🎓"),
    FACULTY ("Faculty",  "👨‍🏫"),
    ADMIN   ("Admin",    "🔑");

    fun canBroadcast() = this == FACULTY || this == ADMIN
    fun canBroadcastAll() = this == ADMIN
}
