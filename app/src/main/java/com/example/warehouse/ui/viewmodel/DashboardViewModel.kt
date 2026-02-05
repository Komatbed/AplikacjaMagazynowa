package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.repository.InventoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DashboardStats(
    val totalItems: Int = 0,
    val totalLength: Int = 0,
    val wasteCount: Int = 0,
    val fullCount: Int = 0,
    val uniqueProfiles: Int = 0
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository(application)

    val stats: StateFlow<DashboardStats> = repository.getItemsFlow()
        .map { items -> calculateStats(items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStats()
        )

    private fun calculateStats(items: List<InventoryItemDto>): DashboardStats {
        val totalItems = items.sumOf { it.quantity }
        val totalLength = items.sumOf { it.lengthMm * it.quantity }
        // Assuming waste is < 6000mm or some heuristic, but user didn't define "waste" vs "full" strictly here.
        // Usually full bars are ~6000mm or 6500mm. Let's say < 6000 is waste.
        // Or check if status is "WASTE" vs "FULL" if that field exists.
        // InventoryItemDto has 'status'.
        
        val wasteCount = items.filter { it.status == "WASTE" || it.lengthMm < 6000 }.sumOf { it.quantity }
        val fullCount = items.filter { it.status == "FULL" || it.lengthMm >= 6000 }.sumOf { it.quantity }
        val uniqueProfiles = items.map { it.profileCode }.distinct().count()

        return DashboardStats(
            totalItems = totalItems,
            totalLength = totalLength,
            wasteCount = wasteCount,
            fullCount = fullCount,
            uniqueProfiles = uniqueProfiles
        )
    }
}
