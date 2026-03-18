package com.campuslink.ui.chat

import androidx.lifecycle.*
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessageStatus
import com.campuslink.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val bluetoothManager: BluetoothManager,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _myId = MutableStateFlow("")
    val myId: StateFlow<String> = _myId.asStateFlow()
    private val _targetId = MutableStateFlow("")
    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _partner = MutableStateFlow<User?>(null)
    val partner: StateFlow<User?> = _partner.asStateFlow()

    init { viewModelScope.launch { _myId.value = sessionManager.getUserId() ?: "" } }
    
    fun load(targetUserId: String) {
        _targetId.value = targetUserId
        viewModelScope.launch {
            repository.ensureUserExists(targetUserId)
            repository.users.collect { list ->
                _partner.value = list.find { it.userId == targetUserId }
            }
        }
        viewModelScope.launch {
            repository.getConversation(targetUserId).collect { _messages.value = it }
        }
    }
    
    fun onText(v: String) { _text.value = v }
    
    fun send() {
        val content = _text.value.trim(); if (content.isBlank()) return
        val myId = _myId.value; if (myId.isBlank()) return
        _text.value = ""
        val msg = Message(
            messageId  = UUID.randomUUID().toString(),
            senderId   = myId,
            receiverId = _targetId.value,
            content    = content,
            timestamp  = System.currentTimeMillis(),
            status     = MessageStatus.SENDING.name
        )
        viewModelScope.launch {
            repository.saveMessage(msg)
            bluetoothManager.sendMessage(msg)
        }
    }
}
