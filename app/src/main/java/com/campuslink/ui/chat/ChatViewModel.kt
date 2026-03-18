package com.campuslink.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.Message
import com.campuslink.domain.model.MessageStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val bluetoothManager: BluetoothManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _myUserId = MutableStateFlow("")
    val myUserId: StateFlow<String> = _myUserId.asStateFlow()

    private val _targetUserId = MutableStateFlow("")
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // --- REALTIME NETWORK BINDINGS ---
    val networkStats = repository.networkStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.campuslink.domain.model.NetworkStats())

    val isConnected = networkStats.map { it.activeNodes > 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeNodes = networkStats.map { it.activeNodes }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val connectionPath = networkStats.map { stats ->
        val nodes = stats.activeNodes
        when {
            nodes == 0 -> "Searching..."
            nodes == 1 -> "Direct Link"
            else -> "Mesh Route ($nodes hops)"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Searching...")

    init {
        viewModelScope.launch {
            _myUserId.value = sessionManager.getUserId() ?: ""
        }
    }

    fun initConversation(targetUserId: String) {
        _targetUserId.value = targetUserId
        viewModelScope.launch {
            repository.getConversation(targetUserId)
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )
                .collect { msgs ->
                    _messages.value = msgs
                }
        }
    }

    fun onMessageTextChange(value: String) {
        _messageText.value = value
    }

    // --- SIMULATION LAYER ---
    var simulationMode = true
        set(value) {
            field = value
            bluetoothManager.setSimulationMode(value)
        }
    private val simA = com.campuslink.simulation.VirtualDevice("A")
    private val simB = com.campuslink.simulation.VirtualDevice("B")
    private val simC = com.campuslink.simulation.VirtualDevice("C")
    private val simD = com.campuslink.simulation.VirtualDevice("D")
    // ------------------------

    fun sendMessage() {
        val content = _messageText.value.trim()
        if (content.isBlank()) return
        val myId = _myUserId.value
        if (myId.isBlank()) return

        _messageText.value = ""

        if (simulationMode) {
            android.util.Log.d("Sim", "Starting Simulation from Chat UI")
            // A sends to D through B and C
            simA.sendMessage("D", content)
            return
        }

        val message = Message(
            messageId = UUID.randomUUID().toString(),
            senderId = myId,
            receiverId = _targetUserId.value,
            content = content,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING.name
        )

        viewModelScope.launch {
            repository.saveMessage(message)
            bluetoothManager.sendMessage(message)
        }
    }
}
