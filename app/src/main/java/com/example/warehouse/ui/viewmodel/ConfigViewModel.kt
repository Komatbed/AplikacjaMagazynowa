package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.local.entity.ColorEntity
import com.example.warehouse.data.local.entity.ProfileEntity
import com.example.warehouse.data.repository.InventoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import kotlinx.coroutines.flow.catch

class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository(application)

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

    fun addProfile(code: String, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addProfile(code, description)
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.deleteProfile(id)
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
    }

    fun addColor(code: String, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.addColor(code, description)
            result.onFailure { _error.value = it.message }
            _isLoading.value = false
        }
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
}
