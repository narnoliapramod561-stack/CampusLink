package com.campuslink.domain.model

data class HandshakePayload(
    val userId: String,
    val username: String,
    val deviceAddress: String,
    val zone: String = "BLOCK_32",
    val role: String = "STUDENT",
    val department: String = ""
)
