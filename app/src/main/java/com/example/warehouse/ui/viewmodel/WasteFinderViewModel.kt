package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.model.InventoryItemDto
import com.example.warehouse.data.repository.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WasteFinderViewModel @JvmOverloads constructor(
    application: Application,
    repo: InventoryRepository? = null
) : AndroidViewModel(application) {
    private val repository = repo ?: InventoryRepository(application)

    private val _result = MutableStateFlow<InventoryItemDto?>(null)
    val result: StateFlow<InventoryItemDto?> = _result.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchStatus = MutableStateFlow<String?>(null)
    val searchStatus: StateFlow<String?> = _searchStatus.asStateFlow()

    // Config for dropdown
    val profiles = repository.getProfilesFlow()

    fun findWaste(profileCode: String, minLength: Int) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchStatus.value = null
            _result.value = null

            try {
                val item = repository.findOptimalWaste(profileCode, minLength)
                if (item != null) {
                    _result.value = item
                    _searchStatus.value = "Znaleziono idealny odpad!"
                } else {
                    _searchStatus.value = "Brak pasującego odpadu. Utnij z nowej sztuki."
                }
            } catch (e: Exception) {
                _searchStatus.value = "Błąd: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clear() {
        _result.value = null
        _searchStatus.value = null
    }
}
