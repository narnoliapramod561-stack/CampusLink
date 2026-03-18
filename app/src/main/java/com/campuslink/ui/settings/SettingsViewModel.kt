package com.campuslink.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.AppSettings
import com.campuslink.simulation.DemoModeEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val demoModeEngine: DemoModeEngine
) : ViewModel() {
    val settings: StateFlow<AppSettings> = sessionManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    
    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()
    
    private val _zone = MutableStateFlow("")
    val zone: StateFlow<String> = _zone.asStateFlow()
    private val _role = MutableStateFlow("")
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
            
            // Auto-activate demo mode if setting is on
            if (sessionManager.settings.first().demoMode) {
                demoModeEngine.activate()
            }
        }
    }
    
    fun updateSettings(s: AppSettings) = viewModelScope.launch { sessionManager.saveSettings(s) }
    
    fun toggleDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) demoModeEngine.activate() else demoModeEngine.deactivate()
            val current = sessionManager.settings.first()
            sessionManager.saveSettings(current.copy(demoMode = enabled))
        }
    }
    
    fun logout(onDone: () -> Unit) = viewModelScope.launch { sessionManager.clearUser(); onDone() }
}
