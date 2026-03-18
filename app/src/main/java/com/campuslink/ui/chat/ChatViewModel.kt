package com.campuslink.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.core.Constants
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessagePriority
import com.campuslink.domain.model.MessageStatus
import com.campuslink.domain.model.MessageTargetType
import com.campuslink.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val bluetoothManager: BluetoothManager,
    private val sessionManager: SessionManager
    // FIX: DemoModeEngine removed. It was intercepting real sends when demoMode=true
    // and routing them to the fake relay simulator instead of real Bluetooth.
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

    private var myZone = "BLOCK_32"
    private var myRole = "STUDENT"

    init {
        viewModelScope.launch {
            _myId.value = sessionManager.getUserId() ?: ""
            myZone = sessionManager.getZone()
            myRole = sessionManager.getRole()
        }
    }

    fun onPriorityChange(p: MessagePriority) { _currentPriority.value = p }

    fun load(targetUserId: String) {
        _targetId.value = targetUserId
        viewModelScope.launch { repository.ensureUserExists(targetUserId) }
        viewModelScope.launch {
            repository.users.collect { list ->
                _partner.value = list.find { it.userId == targetUserId }
            }
        }
        viewModelScope.launch {
            repository.getConversation(targetUserId).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun onText(v: String) { _text.value = v }

    fun send(targetType: String = MessageTargetType.USER.name) {
        val content = _text.value.trim()
        if (content.isBlank()) return
        val myId = _myId.value
        if (myId.isBlank()) return
        val targetId = _targetId.value
        if (targetId.isBlank()) return

        _text.value = ""

        val msg = Message(
            messageId  = UUID.randomUUID().toString(),
            senderId   = myId,
            receiverId = targetId,
            content    = content,
            timestamp  = System.currentTimeMillis(),
            status     = MessageStatus.SENDING.name,
            targetType = targetType,
            priority   = _currentPriority.value.name,
            senderZone = myZone,
            senderRole = myRole,
            ttl        = when (_currentPriority.value) {
                             MessagePriority.EMERGENCY -> 15
                             MessagePriority.IMPORTANT -> 9
                             else -> Constants.DEFAULT_TTL
                         }
        )

        viewModelScope.launch {
            // Save to DB first → message appears in UI immediately
            repository.saveMessage(msg)
            // Then transmit via Bluetooth
            bluetoothManager.sendMessage(msg)
        }
    }
}
