package com.campuslink.simulation

fun getNextHop(current: String): String {
    return when (current) {
        "A" -> "B"
        "B" -> "C"
        "C" -> "D"
        else -> ""
    }
}

fun getPreviousHop(current: String): String {
    return when (current) {
        "D" -> "C"
        "C" -> "B"
        "B" -> "A"
        else -> ""
    }
}
