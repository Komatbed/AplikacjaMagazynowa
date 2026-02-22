package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.model.LocationStatusDto
import com.example.warehouse.data.model.PalletDetailsDto
import com.example.warehouse.data.repository.InventoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class WarehouseMapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository(application)

    private val _locations = mutableStateOf<List<LocationStatusDto>>(emptyList())
    val locations: State<List<LocationStatusDto>> = _locations

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _selectedLocation = mutableStateOf<LocationStatusDto?>(null)
    val selectedLocation: State<LocationStatusDto?> = _selectedLocation

    private val _locationItems = mutableStateOf<List<InventoryItemDto>>(emptyList())
    val locationItems: State<List<InventoryItemDto>> = _locationItems

    private val _selectedPalletDetails = mutableStateOf<PalletDetailsDto?>(null)
    val selectedPalletDetails: State<PalletDetailsDto?> = _selectedPalletDetails

    private val _isPalletDetailsLoading = mutableStateOf(false)
    val isPalletDetailsLoading: State<Boolean> = _isPalletDetailsLoading

    private var itemsJob: Job? = null

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

    fun updateCapacity(id: Int, capacity: Int) {
        viewModelScope.launch {
            repository.updateLocationCapacity(id, capacity).onSuccess {
                loadMap()
            }.onFailure {
                _error.value = "Błąd aktualizacji pojemności: ${it.message}"
            }
        }
    }

    fun selectLocation(location: LocationStatusDto?) {
        _selectedLocation.value = location
        itemsJob?.cancel()
        if (location == null || location.label.isNullOrEmpty()) {
            _locationItems.value = emptyList()
            _selectedPalletDetails.value = null
            _isPalletDetailsLoading.value = false
            return
        }

        itemsJob = viewModelScope.launch {
            try {
                repository.refreshItems(location = location.label)
            } catch (_: Exception) {
            }

            repository.getItemsFlow(location = location.label).collect { items ->
                _locationItems.value = items
            }
        }

        viewModelScope.launch {
            _isPalletDetailsLoading.value = true
            _error.value = null
            val result = repository.getPalletDetails(location.label)
            result.onSuccess {
                _selectedPalletDetails.value = it
            }.onFailure {
                _error.value = "Błąd pobierania szczegółów palety: ${it.message}"
            }
            _isPalletDetailsLoading.value = false
        }
    }
}
