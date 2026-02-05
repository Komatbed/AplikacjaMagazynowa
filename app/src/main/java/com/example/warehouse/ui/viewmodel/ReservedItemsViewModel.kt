package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ReservedItemsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository(application)

    private val _items = MutableStateFlow<List<InventoryItemDto>>(emptyList())
    val items: StateFlow<List<InventoryItemDto>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Filters
    private val _filterUser = MutableStateFlow("")
    val filterUser: StateFlow<String> = _filterUser.asStateFlow()

    private val _filterDate = MutableStateFlow("")
    val filterDate: StateFlow<String> = _filterDate.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            repository.getItemsFlow()
                .catch { e -> 
                    _error.value = "Błąd pobierania: ${e.message}"
                    _isLoading.value = false
                }
                .collect { allItems ->
                    val filtered = allItems.filter { item ->
                        (item.status == "RESERVED" || item.status == "IN_PROGRESS") &&
                        (filterUser.value.isEmpty() || item.reservedBy?.contains(filterUser.value, ignoreCase = true) == true) &&
                        (filterDate.value.isEmpty() || item.reservationDate?.contains(filterDate.value) == true)
                    }
                    _items.value = filtered
                    _isLoading.value = false
                }
        }
    }

    fun updateFilterUser(query: String) {
        _filterUser.value = query
        loadItems() // Trigger reload/re-filter
    }

    fun updateFilterDate(date: String) {
        _filterDate.value = date
        loadItems()
    }
}
