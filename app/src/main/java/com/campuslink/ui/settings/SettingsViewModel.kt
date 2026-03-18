package com.campuslink.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.AppSettings
import com.campuslink.domain.model.LpuZone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager
    // FIX: DemoModeEngine removed
) : ViewModel() {

    val settings: StateFlow<AppSettings> = sessionManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _zone = MutableStateFlow(LpuZone.BLOCK_32.name)
    val zone: StateFlow<String> = _zone.asStateFlow()

    private val _role = MutableStateFlow("STUDENT")
    val role: StateFlow<String> = _role.asStateFlow()

    private val _department = MutableStateFlow("")
    val department: StateFlow<String> = _department.asStateFlow()

    init {
        viewModelScope.launch {
            _userId.value     = sessionManager.getUserId() ?: ""
            _username.value   = sessionManager.getUsername() ?: ""
            _zone.value       = sessionManager.getZone()
            _role.value       = sessionManager.getRole()
            _department.value = sessionManager.getDept()
        }
    }

    fun updateSettings(s: AppSettings) = viewModelScope.launch {
        sessionManager.saveSettings(s)
    }

    fun updateZone(zone: String) = viewModelScope.launch {
        _zone.value = zone
        sessionManager.saveProfile(
            userId   = _userId.value,
            username = _username.value,
            zone     = zone,
            role     = _role.value,
            dept     = _department.value
        )
    }

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        sessionManager.clearUser()
        onDone()
    }
}
