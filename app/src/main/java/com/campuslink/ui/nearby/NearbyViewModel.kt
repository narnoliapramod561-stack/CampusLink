package com.campuslink.ui.nearby

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.NetworkStats
import com.campuslink.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    
    val users: StateFlow<List<User>> = repository.users
        .combine(_myId) { users, myId -> users.filter { it.userId != myId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val isRunning: StateFlow<Boolean> = bluetoothManager.isRunning
    
    val stats: StateFlow<NetworkStats> = repository.networkStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkStats())
    
    init { viewModelScope.launch { _myId.value = sessionManager.getUserId() ?: "" } }
    
    fun refresh() { bluetoothManager.stop(); bluetoothManager.start() }
}
