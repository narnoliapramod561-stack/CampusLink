package com.campuslink.domain.model

sealed class ConnectionEvent {
    data class PeerConnected(val peer: User) : ConnectionEvent()
    data class PeerDisconnected(val userId: String) : ConnectionEvent()
    data class MessageReceived(val msg: Message) : ConnectionEvent()
    data class AckReceived(val ack: AckPayload) : ConnectionEvent()
    data class Error(val type: ErrorType, val detail: String) : ConnectionEvent()
}
