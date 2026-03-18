package com.campuslink.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(private val sessionManager: SessionManager) : ViewModel() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        viewModelScope.launch {
            if (sessionManager.isLoggedIn()) _isLoggedIn.value = true
        }
    }

    fun login(username: String) {
        viewModelScope.launch {
            val userId = UUID.randomUUID().toString().take(8)
            sessionManager.saveUser(userId, username)
            _isLoggedIn.value = true
        }
    }
}
