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
    
    val zone = MutableStateFlow(com.campuslink.domain.model.LpuZone.BLOCK_32.name)
    val role = MutableStateFlow(com.campuslink.domain.model.UserRole.STUDENT.name)
    val department = MutableStateFlow("")

    init {
        viewModelScope.launch {
            if (sessionManager.isLoggedIn()) _isLoggedIn.value = true
        }
    }

    fun onZone(z: String) { zone.value = z }
    fun onRole(r: String) { role.value = r }
    fun onDepartment(d: String) { department.value = d }

    fun login(username: String) {
        viewModelScope.launch {
            val userId = UUID.randomUUID().toString().take(8)
            sessionManager.saveProfile(userId, username, zone.value, role.value, department.value)
            _isLoggedIn.value = true
        }
    }
}
