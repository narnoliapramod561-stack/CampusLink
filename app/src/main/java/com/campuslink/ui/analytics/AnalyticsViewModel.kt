package com.campuslink.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campuslink.data.repository.ChatRepository
import com.campuslink.domain.model.NetworkStats
import com.campuslink.domain.model.PerformanceLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {
    val stats: StateFlow<NetworkStats> = repository.networkStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetworkStats())
        
    val logs: StateFlow<List<PerformanceLog>> = repository.getRecentLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clearLogs() { viewModelScope.launch { repository.clearLogs() } }
}
