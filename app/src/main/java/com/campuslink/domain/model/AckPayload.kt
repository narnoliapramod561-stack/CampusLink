package com.campuslink.domain.model

data class AckPayload(val messageId: String, val originalSenderId: String, val receiverUserId: String)
