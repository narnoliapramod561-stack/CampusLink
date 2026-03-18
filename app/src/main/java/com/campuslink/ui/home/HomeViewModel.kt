package com.campuslink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.ConversationPreview
import com.campuslink.domain.model.NetworkStats
import com.campuslink.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val bluetoothManager: BluetoothManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    // ── My identity ───────────────────────────────────────────────────────
    private val _myUserId = MutableStateFlow("")
    val myUserId: StateFlow<String> = _myUserId.asStateFlow()

    private val _myUsername = MutableStateFlow("")
    val myUsername: StateFlow<String> = _myUsername.asStateFlow()

    // ── BLE-discovered nearby users ───────────────────────────────────────
    val nearbyUsers: StateFlow<List<User>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Conversation previews (WhatsApp-style previous chats) ─────────────
    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<ConversationPreview>> = _myUserId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList())
            else repository.getConversationPreviews(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── Network stats ─────────────────────────────────────────────────────
    val networkStats: StateFlow<NetworkStats> = repository.networkStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkStats())

    val isBluetoothRunning: StateFlow<Boolean> = bluetoothManager.isRunning

    // ── Manual connect dialog state ───────────────────────────────────────
    private val _showConnectDialog = MutableStateFlow(false)
    val showConnectDialog: StateFlow<Boolean> = _showConnectDialog.asStateFlow()

    private val _connectInput = MutableStateFlow("")
    val connectInput: StateFlow<String> = _connectInput.asStateFlow()

    private val _connectError = MutableStateFlow<String?>(null)
    val connectError: StateFlow<String?> = _connectError.asStateFlow()

    // Emits the userId to navigate to when a manual connect succeeds
    private val _navigateToChat = MutableStateFlow<Pair<String,String>?>(null)
    val navigateToChat: StateFlow<Pair<String,String>?> = _navigateToChat.asStateFlow()

    init {
        viewModelScope.launch {
            _myUserId.value   = sessionManager.getUserId()   ?: ""
            _myUsername.value = sessionManager.getUsername() ?: ""
        }
    }

    // ── Manual connect actions ────────────────────────────────────────────

    fun openConnectDialog() {
        _connectInput.value = ""
        _connectError.value = null
        _showConnectDialog.value = true
    }

    fun dismissConnectDialog() {
        _showConnectDialog.value = false
    }

    fun onConnectInputChange(value: String) {
        _connectInput.value = value
        _connectError.value = null
    }

    fun confirmConnect() {
        val userId = _connectInput.value.trim()
        if (userId.isBlank()) {
            _connectError.value = "Please enter a User ID"
            return
        }
        if (!userId.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _connectError.value = "User ID must be alphanumeric only"
            return
        }
        if (userId == _myUserId.value) {
            _connectError.value = "That's your own User ID"
            return
        }
        viewModelScope.launch {
            // Ensure the user exists in DB so conversation shows in list
            repository.ensureUserExists(userId)
            _showConnectDialog.value = false
            _connectInput.value = ""
            // Trigger navigation to chat screen
            _navigateToChat.value = Pair(userId, userId)
        }
    }

    fun onNavigatedToChat() {
        _navigateToChat.value = null
    }

    fun refreshScan() {
        if (!bluetoothManager.isRunning.value) {
            bluetoothManager.start()
        }
    }
}
