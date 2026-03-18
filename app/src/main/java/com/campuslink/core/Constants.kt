package com.campuslink.core

import java.util.UUID

object Constants {
    val MY_APP_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    const val DEFAULT_TTL = 7
    const val HEARTBEAT_INTERVAL_MS = 10_000L
    const val HEARTBEAT_TIMEOUT_MS = 15_000L
    const val MAX_PROCESSED_IDS = 1000
    const val PROCESSED_ID_EXPIRY_MS = 86_400_000L
    const val MESSAGE_EXPIRY_MS = 86_400_000L
    const val MAX_RETRY_ATTEMPTS = 3
    val RETRY_DELAYS_MS = listOf(1_000L, 3_000L, 9_000L)
    const val BT_SERVICE_NAME = "CampusLink"
    const val NOTIFICATION_CHANNEL_ID = "campuslink_bt"
}
