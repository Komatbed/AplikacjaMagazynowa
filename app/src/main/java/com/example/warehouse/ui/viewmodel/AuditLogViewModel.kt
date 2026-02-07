package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.repository.AuditRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuditLogViewModel(application: Application) : AndroidViewModel(application) {
    private val auditRepository = AuditRepository(application)
    
    private val _filter = MutableStateFlow("ALL") // ALL, CONFIG, INVENTORY
    val filter = _filter.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ALL")

    private val _allLogs = auditRepository.getAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs = combine(_allLogs, _filter) { logs, filterType ->
        if (filterType == "ALL") logs else logs.filter { it.itemType == filterType }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats = _allLogs.combine(_filter) { logs, _ ->
        val configCount = logs.count { it.itemType == "CONFIG" }
        val inventoryCount = logs.count { it.itemType == "INVENTORY" }
        val todayCount = logs.count { 
            val now = System.currentTimeMillis()
            it.timestamp > now - 24 * 60 * 60 * 1000 
        }
        AuditStats(configCount, inventoryCount, todayCount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AuditStats(0, 0, 0))

    fun setFilter(type: String) {
        _filter.value = type
    }

    fun clearLogs() {
        viewModelScope.launch {
            auditRepository.clearAuditLogs()
        }
    }
}

data class AuditStats(
    val configChanges: Int,
    val inventoryOps: Int,
    val actionsToday: Int
)
