package com.campuslink.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.NetworkStats
import com.campuslink.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NearbyViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val bluetoothManager: BluetoothManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _myId = MutableStateFlow("")
    val myId: StateFlow<String> = _myId.asStateFlow()

    // FIX: Show ONLY users whose userId is in the live connectedUserIds set.
    // Previously this showed ALL users from the DB (including users from past sessions
    // who are no longer physically nearby). Now it strictly shows who is actively
    // connected via Bluetooth right now — i.e., truly "in range".
    val users: StateFlow<List<User>> = combine(
        repository.users,
        bluetoothManager.connectedUserIds,
        _myId
    ) { allUsers, connectedIds, myId ->
        allUsers.filter { user ->
            user.userId != myId &&          // not myself
            user.userId in connectedIds     // actually connected right now
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Count of physically connected peers (for status display)
    val connectedCount: StateFlow<Int> = bluetoothManager.connectedUserIds
        .combine(_myId) { ids, myId -> ids.count { it != myId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val isRunning: StateFlow<Boolean> = bluetoothManager.isRunning

    val stats: StateFlow<NetworkStats> = repository.networkStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkStats())

    init {
        viewModelScope.launch { _myId.value = sessionManager.getUserId() ?: "" }
    }

    fun refresh() {
        // Stop and restart Bluetooth to force a fresh BLE scan cycle
        bluetoothManager.stop()
        bluetoothManager.start()
    }
}
