package com.campuslink.domain.model

data class NetworkStats(
    val activeNodes: Int = 0,
    val messagesSent: Int = 0,
    val messagesRelayed: Int = 0,
    val messagesDelivered: Int = 0,
    val messagesPending: Int = 0,
    val avgHopCount: Float = 0f,
    val avgLatencyMs: Long = 0L,
    val relaySuccessRate: Float = 1f
)
