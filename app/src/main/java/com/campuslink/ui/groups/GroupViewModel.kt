package com.campuslink.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.bluetooth.BluetoothManager
import com.campuslink.data.repository.ChatRepository
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.LpuGroup
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val bluetoothManager: BluetoothManager,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _myId = MutableStateFlow("")
    val myId: StateFlow<String> = _myId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val groups: StateFlow<List<LpuGroup>> = repository.getAllGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()
    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    init { viewModelScope.launch { _myId.value = sessionManager.getUserId() ?: "" } }

    fun openCreate() { _groupName.value = ""; _showCreateDialog.value = true }
    fun dismissCreate() { _showCreateDialog.value = false }
    fun onGroupName(v: String) { _groupName.value = v }

    fun createGroup() {
        val name = _groupName.value.trim(); if (name.isBlank()) return
        val myId = _myId.value
        val group = LpuGroup(
            groupId   = UUID.randomUUID().toString(),
            name      = if (name.startsWith("#")) name else "#$name",
            createdBy = myId,
            memberIds = Gson().toJson(listOf(myId)),
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.upsertGroup(group)
            _showCreateDialog.value = false
        }
    }

    fun joinGroup(groupId: String) {
        viewModelScope.launch {
            val group = repository.getGroupById(groupId) ?: return@launch
            val members = try { Gson().fromJson(group.memberIds, Array<String>::class.java).toMutableList() }
                          catch (_: Exception) { mutableListOf() }
            if (_myId.value !in members) {
                members.add(_myId.value)
                repository.upsertGroup(group.copy(memberIds = Gson().toJson(members)))
            }
        }
    }
}
