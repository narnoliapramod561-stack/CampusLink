package com.campuslink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.AppSettings
import com.campuslink.domain.model.ConversationPreview
import com.campuslink.domain.model.NetworkStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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

    private val _myUserId = MutableStateFlow("")
    val myUserId: StateFlow<String> = _myUserId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<ConversationPreview>> = _myUserId
        .flatMapLatest { id ->
            if (id.isBlank()) flowOf(emptyList())
            else repository.getConversationPreviews(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkStats: StateFlow<NetworkStats> = repository.networkStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkStats())

    val isBluetoothRunning: StateFlow<Boolean> = bluetoothManager.isRunning

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()
    private val _dialogInput = MutableStateFlow("")
    val dialogInput: StateFlow<String> = _dialogInput.asStateFlow()
    private val _dialogError = MutableStateFlow<String?>(null)
    val dialogError: StateFlow<String?> = _dialogError.asStateFlow()
    private val _navigate = MutableStateFlow<Pair<String, String>?>(null)
    val navigate: StateFlow<Pair<String, String>?> = _navigate.asStateFlow()

    val settingsFlow: Flow<AppSettings> = sessionManager.settings

    init {
        viewModelScope.launch {
            _myUserId.value = sessionManager.getUserId() ?: ""

            // ── BUG FIX ────────────────────────────────────────────────────
            // Removed bluetoothManager.start() from here.
            //
            // BluetoothForegroundService.onStartCommand is the ONLY authoritative
            // place to call start(). It runs on the main thread immediately when
            // the service starts, guaranteeing relayEngine.bluetoothManager is
            // set before any BLE discovery or RFCOMM connections begin.
            //
            // Having HomeViewModel also call start() created a race:
            //   - If ViewModel wins: service later calls start() (no-op due to
            //     AtomicBoolean guard) BUT relayEngine.bluetoothManager was already
            //     null during the first connections → Nearby screen stayed empty.
            //
            // The AtomicBoolean guard in BluetoothManager ensures the service's
            // start() call is always a safe no-op if Bluetooth already running.
            // The FIX in BluetoothManager.init (relayEngine.bluetoothManager = this)
            // means this race no longer matters, but removing the call here is
            // still cleaner architecture.
            // ──────────────────────────────────────────────────────────────
        }
    }

    fun openDialog() {
        _dialogInput.value = ""
        _dialogError.value = null
        _showDialog.value = true
    }

    fun dismissDialog() { _showDialog.value = false }

    fun onDialogInput(v: String) {
        _dialogInput.value = v
        _dialogError.value = null
    }

    fun confirmConnect() {
        val id = _dialogInput.value.trim()
        when {
            id.isBlank() -> _dialogError.value = "Enter a User ID"
            !id.matches(Regex("^[a-zA-Z0-9_]+$")) -> _dialogError.value = "Alphanumeric only"
            id == _myUserId.value -> _dialogError.value = "That's your own ID"
            else -> viewModelScope.launch {
                repository.ensureUserExists(id)
                _showDialog.value = false
                _navigate.value = Pair(id, id)
            }
        }
    }

    fun onNavigated() { _navigate.value = null }

    // Only restart if Bluetooth was explicitly stopped
    fun refresh() {
        if (!bluetoothManager.isRunning.value) bluetoothManager.start()
    }
}
