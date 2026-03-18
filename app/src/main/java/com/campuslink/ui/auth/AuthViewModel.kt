package com.campuslink.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AuthUiState(
    val username: String = "",
    val userId: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    // ── BUG B3 FIX ────────────────────────────────────────────────────────
    // New state: null = still checking session, true = already logged in,
    // false = not logged in → show form. AuthScreen reads this to decide
    // whether to navigate immediately or show the registration card.
    // ──────────────────────────────────────────────────────────────────────
    val sessionChecked: Boolean = false,
    val alreadyLoggedIn: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(userId = "user_" + UUID.randomUUID().toString().take(8))
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Check existing session immediately on ViewModel creation
        viewModelScope.launch {
            val loggedIn = sessionManager.isLoggedIn()
            _uiState.value = _uiState.value.copy(
                sessionChecked = true,
                alreadyLoggedIn = loggedIn
            )
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.value = _uiState.value.copy(username = value, error = null)
    }

    fun onUserIdChange(value: String) {
        _uiState.value = _uiState.value.copy(userId = value, error = null)
    }

    fun joinNetwork() {
        val state = _uiState.value
        if (state.username.isBlank()) {
            _uiState.value = state.copy(error = "Username cannot be empty")
            return
        }
        if (state.userId.isBlank()) {
            _uiState.value = state.copy(error = "User ID cannot be empty")
            return
        }
        if (!state.userId.matches(Regex("^[a-zA-Z0-9_]+$"))) {
            _uiState.value = state.copy(error = "User ID must be alphanumeric only")
            return
        }
        _uiState.value = state.copy(isLoading = true)
        viewModelScope.launch {
            sessionManager.saveUser(state.userId, state.username)
            _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
        }
    }
}
