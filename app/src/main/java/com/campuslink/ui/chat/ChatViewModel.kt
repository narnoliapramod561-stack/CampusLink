package com.campuslink.ui.chat

import androidx.lifecycle.*
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.*
import com.campuslink.simulation.DemoModeEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val bluetoothManager: BluetoothManager,
    private val sessionManager: SessionManager,
    private val demoModeEngine: DemoModeEngine
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

    private val _currentPriority = MutableStateFlow(MessagePriority.NORMAL)
    val currentPriority: StateFlow<MessagePriority> = _currentPriority.asStateFlow()
    fun onPriorityChange(p: MessagePriority) { _currentPriority.value = p }

    var myZone = ""; var myRole = "STUDENT"

    init { 
        viewModelScope.launch { 
            _myId.value = sessionManager.getUserId() ?: ""
            myZone = sessionManager.getZone()
            myRole = sessionManager.getRole()
        } 
    }
    
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
    
    fun send(targetType: String = MessageTargetType.USER.name) {
        val content = _text.value.trim(); if (content.isBlank()) return
        val myId = _myId.value; if (myId.isBlank()) return
        _text.value = ""
        val msg = Message(
            messageId  = UUID.randomUUID().toString(),
            senderId   = myId,
            receiverId = _targetId.value,
            content    = content,
            timestamp  = System.currentTimeMillis(),
            status     = MessageStatus.SENDING.name,
            targetType = targetType,
            priority   = _currentPriority.value.name,
            senderZone = myZone,
            senderRole = myRole,
            ttl        = when(_currentPriority.value) {
                             MessagePriority.EMERGENCY -> 15
                             MessagePriority.IMPORTANT -> 9
                             else -> com.campuslink.core.Constants.DEFAULT_TTL
                         }
        )
        viewModelScope.launch {
            val settings = sessionManager.settings.first()
            if (settings.demoMode && demoModeEngine.isActive() && demoModeEngine.getFakeUsers().any { it.userId == msg.receiverId }) {
                demoModeEngine.simulateRelay(content, myId, msg.receiverId)
            } else {
                repository.saveMessage(msg)
                bluetoothManager.sendMessage(msg)
            }
        }
    }
}
