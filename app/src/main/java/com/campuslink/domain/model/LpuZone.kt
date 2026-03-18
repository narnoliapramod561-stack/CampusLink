package com.campuslink.domain.model

enum class LpuZone(val displayName: String, val emoji: String) {
    LIBRARY     ("Shanti Devi Mittal Library", "📚"),
    BLOCK_32    ("Block 32 — CSE/IT",          "💻"),
    BLOCK_34    ("Block 34 — Engineering",     "⚙️"),
    BLOCK_38    ("Block 38 — Management",      "📊"),
    CANTEEN     ("University Canteen",         "🍽️"),
    AUDITORIUM  ("Uni Auditorium",             "🎭"),
    HOSTEL_BOYS ("Boys Hostel Zone",           "🏠"),
    HOSTEL_GIRLS("Girls Hostel Zone",          "🏡"),
    GROUND      ("Sports Ground",             "⚽"),
    MEDICAL     ("Medical Centre",            "🏥");

    companion object {
        fun fromName(name: String) = values().firstOrNull { it.name == name } ?: BLOCK_32
    }
}
