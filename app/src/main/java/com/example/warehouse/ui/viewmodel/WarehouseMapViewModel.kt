package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.model.LocationStatusDto
import com.example.warehouse.data.repository.InventoryRepository
import kotlinx.coroutines.launch

class WarehouseMapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository(application)

    private val _locations = mutableStateOf<List<LocationStatusDto>>(emptyList())
    val locations: State<List<LocationStatusDto>> = _locations

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        loadMap()
    }

    fun loadMap() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val result = repository.getWarehouseMap()
            result.onSuccess {
                _locations.value = it
            }.onFailure {
                _error.value = "Błąd pobierania mapy: ${it.message}"
            }
            
            _isLoading.value = false
        }
    }
}
