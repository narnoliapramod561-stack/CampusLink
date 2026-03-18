package com.campuslink.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.data.session.SessionManager
import com.campuslink.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    val settings: StateFlow<AppSettings> = sessionManager.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    
    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    init {
        viewModelScope.launch {
            _userId.value   = sessionManager.getUserId() ?: ""
            _username.value = sessionManager.getUsername() ?: ""
        }
    }
    fun updateSettings(s: AppSettings) = viewModelScope.launch { sessionManager.saveSettings(s) }
    fun logout(onDone: () -> Unit) = viewModelScope.launch { sessionManager.clearUser(); onDone() }
}
