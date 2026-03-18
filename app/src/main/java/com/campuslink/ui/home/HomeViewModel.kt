package com.campuslink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.NetworkStats
import com.campuslink.domain.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SimNode { A, B, C, D, NONE }

data class SimState(
    val packetPos: Float = -1f, // 0.0 (A) to 1.0 (D)
    val activeNode: SimNode = SimNode.NONE,
    val isAck: Boolean = false,
    val isRunning: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val bluetoothManager: BluetoothManager,
    private val sessionManager: SessionManager
) : ViewModel() {

    val users: StateFlow<List<User>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val networkStats: StateFlow<NetworkStats> = repository.networkStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkStats())

    val isBluetoothRunning: StateFlow<Boolean> = bluetoothManager.isRunning

    private val _myUserId = MutableStateFlow("")
    val myUserId: StateFlow<String> = _myUserId.asStateFlow()

    // --- SIMULATION UI STATE ---
    private val _simState = MutableStateFlow(SimState())
    val simState: StateFlow<SimState> = _simState.asStateFlow()

    init {
        viewModelScope.launch {
            _myUserId.value = sessionManager.getUserId() ?: ""
        }
    }

    fun startRelayDemo() {
        if (_simState.value.isRunning) return
        
        viewModelScope.launch {
            _simState.value = SimState(isRunning = true, activeNode = SimNode.A, packetPos = 0f)
            
            // Forward Path: A -> B -> C -> D
            animatePath(SimNode.A, SimNode.B, false)
            animatePath(SimNode.B, SimNode.C, false)
            animatePath(SimNode.C, SimNode.D, false)
            
            _simState.value = _simState.value.copy(activeNode = SimNode.D)
            delay(500) // Processing at D
            
            // Backward Path (ACK): D -> C -> B -> A
            animatePath(SimNode.D, SimNode.C, true)
            animatePath(SimNode.C, SimNode.B, true)
            animatePath(SimNode.B, SimNode.A, true)
            
            _simState.value = _simState.value.copy(activeNode = SimNode.A)
            delay(500)
            _simState.value = SimState(isRunning = false)
        }
    }

    private suspend fun animatePath(from: SimNode, to: SimNode, isAck: Boolean) {
        val fromPos = nodeToPos(from)
        val toPos = nodeToPos(to)
        val steps = 20
        _simState.value = _simState.value.copy(activeNode = from, isAck = isAck)
        
        for (i in 0..steps) {
            val progress = i.toFloat() / steps
            val currentPos = fromPos + (toPos - fromPos) * progress
            _simState.value = _simState.value.copy(packetPos = currentPos)
            delay(40)
        }
    }

    private fun nodeToPos(node: SimNode): Float = when(node) {
        SimNode.A -> 0f
        SimNode.B -> 0.33f
        SimNode.C -> 0.66f
        SimNode.D -> 1f
        else -> 0f
    }

    fun refreshScan() {
        if (!bluetoothManager.isRunning.value) {
            bluetoothManager.start()
        }
    }
}
