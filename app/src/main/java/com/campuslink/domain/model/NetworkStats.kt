package com.campuslink.domain.model

data class NetworkStats(
    val activeNodes: Int = 0,
    val messagesSent: Int = 0,
    val messagesRelayed: Int = 0,
    val messagesDelivered: Int = 0,
    val avgHopCount: Float = 0f
)
