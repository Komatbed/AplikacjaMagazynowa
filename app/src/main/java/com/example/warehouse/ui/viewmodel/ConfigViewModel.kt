package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.local.entity.ColorEntity
import com.example.warehouse.data.local.entity.ProfileEntity
import com.example.warehouse.data.repository.ConfigRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.catch

class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ConfigRepository(application)

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    val profiles = repository.getProfilesFlow()
        .catch { e -> _error.value = "Błąd bazy: ${e.message}"; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val colors = repository.getColorsFlow()
        .catch { e -> _error.value = "Błąd bazy: ${e.message}"; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.refreshConfig()
            _isLoading.value = false
        }
    }

    fun addProfile(profile: com.example.warehouse.data.model.ProfileDefinition) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addProfile(profile)
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateProfile(profile: com.example.warehouse.data.model.ProfileDefinition) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateProfile(profile)
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun addProfile(code: String, description: String) {
        addProfile(com.example.warehouse.data.model.ProfileDefinition(code = code, description = description))
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteProfile(id)
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun addColor(color: com.example.warehouse.data.model.ColorDefinition) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addColor(color)
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun updateColor(color: com.example.warehouse.data.model.ColorDefinition) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.updateColor(color)
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun addColor(code: String, description: String) {
        addColor(com.example.warehouse.data.model.ColorDefinition(code = code, description = description))
    }

    fun deleteColor(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteColor(id)
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun exportConfig(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val json = repository.exportConfig()
                onResult(json)
            } catch (e: Exception) {
                _error.value = "Eksport nieudany: ${e.message}"
                onResult(null)
            }
            _isLoading.value = false
        }
    }

    fun importConfig(json: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.importConfig(json)
            result.onFailure { _error.value = "Import nieudany: ${it.message}" }
            result.onSuccess { refresh() }
            _isLoading.value = false
        }
    }
}
