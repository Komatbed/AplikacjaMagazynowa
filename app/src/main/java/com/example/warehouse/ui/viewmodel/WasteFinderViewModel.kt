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

import com.example.warehouse.data.local.WarehouseDatabase
import com.example.warehouse.data.local.entity.PresetEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class WasteFinderViewModel @JvmOverloads constructor(
    application: Application,
    repo: InventoryRepository? = null
) : AndroidViewModel(application) {
    private val repository = repo ?: InventoryRepository(application)
    private val presetDao = WarehouseDatabase.getDatabase(application).presetDao()

    private val _result = MutableStateFlow<InventoryItemDto?>(null)
    val result: StateFlow<InventoryItemDto?> = _result.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchStatus = MutableStateFlow<String?>(null)
    val searchStatus: StateFlow<String?> = _searchStatus.asStateFlow()

    // Config Data
    val profiles = repository.getProfilesFlow()
    val colors = repository.getColorsFlow()
    val presets = presetDao.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun findWaste(profileCode: String, minLength: Int, externalColor: String? = null, internalColor: String? = null) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchStatus.value = null
            _result.value = null

            try {
                val item = repository.findOptimalWaste(profileCode, minLength, externalColor, internalColor)
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

    fun savePreset(name: String, profileCode: String, externalColor: String, internalColor: String) {
        viewModelScope.launch {
            presetDao.insert(
                PresetEntity(
                    name = name,
                    profileCode = profileCode,
                    externalColor = externalColor,
                    internalColor = internalColor
                )
            )
        }
    }

    fun deletePreset(preset: PresetEntity) {
        viewModelScope.launch {
            presetDao.delete(preset)
        }
    }

    fun clear() {
        _result.value = null
        _searchStatus.value = null
    }
}
