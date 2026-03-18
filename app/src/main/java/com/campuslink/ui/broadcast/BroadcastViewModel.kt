package com.campuslink.ui.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.core.Constants
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.LpuZone
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessagePriority
import com.campuslink.domain.model.MessageTargetType
import com.campuslink.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val bluetoothManager: BluetoothManager,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _myId = MutableStateFlow("")
    private val _myRole = MutableStateFlow("STUDENT")
    private val _myZone = MutableStateFlow("BLOCK_32")
    val myRole: StateFlow<String> = _myRole.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val broadcastMessages: StateFlow<List<Message>> = repository.getBroadcasts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val zoneMessages: StateFlow<List<Message>> = _myZone.flatMapLatest { zone ->
        repository.getZoneMessages(zone)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usersPerZone: StateFlow<Map<String, Int>> = repository.users
        .map { users -> LpuZone.values().associate { z -> z.name to users.count { it.zone == z.name && it.isOnline } } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()
    private val _selectedZone = MutableStateFlow<LpuZone?>(null)
    val selectedZone: StateFlow<LpuZone?> = _selectedZone.asStateFlow()
    private val _priority = MutableStateFlow(MessagePriority.NORMAL)
    val priority: StateFlow<MessagePriority> = _priority.asStateFlow()
    private val _tab = MutableStateFlow(0)  // 0=Zone, 1=Broadcast
    val tab: StateFlow<Int> = _tab.asStateFlow()

    init {
        viewModelScope.launch {
            _myId.value    = sessionManager.getUserId() ?: ""
            _myRole.value  = sessionManager.getRole()
            _myZone.value  = sessionManager.getZone()
            _selectedZone.value = LpuZone.fromName(_myZone.value)
        }
    }

    fun onContent(v: String) { _content.value = v }
    fun onZone(z: LpuZone) { _selectedZone.value = z }
    fun onPriority(p: MessagePriority) { _priority.value = p }
    fun onTab(t: Int) { _tab.value = t }

    fun canBroadcast() = UserRole.valueOf(_myRole.value).canBroadcast()

    fun sendToZone() {
        val zone = _selectedZone.value ?: return
        val msgContent = _content.value.trim(); if (msgContent.isBlank()) return
        val myId = _myId.value; if (myId.isBlank()) return
        _content.value = ""
        val msg = Message(
            messageId  = UUID.randomUUID().toString(),
            senderId   = myId,
            receiverId = zone.name,
            content    = msgContent,
            timestamp  = System.currentTimeMillis(),
            targetType = MessageTargetType.ZONE.name,
            priority   = _priority.value.name,
            senderZone = _myZone.value,
            senderRole = _myRole.value,
            ttl        = if (_priority.value == MessagePriority.EMERGENCY) 15 else Constants.DEFAULT_TTL
        )
        viewModelScope.launch {
            repository.saveMessage(msg)
            bluetoothManager.sendMessage(msg)
        }
    }

    fun sendBroadcast() {
        if (!canBroadcast()) return
        val msgContent = _content.value.trim(); if (msgContent.isBlank()) return
        val myId = _myId.value; if (myId.isBlank()) return
        _content.value = ""
        val msg = Message(
            messageId  = UUID.randomUUID().toString(),
            senderId   = myId,
            receiverId = "ALL",
            content    = msgContent,
            timestamp  = System.currentTimeMillis(),
            targetType = MessageTargetType.BROADCAST.name,
            priority   = _priority.value.name,
            senderZone = _myZone.value,
            senderRole = _myRole.value,
            ttl        = 15  // broadcasts always use max TTL
        )
        viewModelScope.launch {
            repository.saveMessage(msg)
            bluetoothManager.sendMessage(msg)
        }
    }
}
