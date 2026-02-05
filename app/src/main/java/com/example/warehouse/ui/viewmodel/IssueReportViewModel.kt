package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.repository.InventoryRepository
import kotlinx.coroutines.launch

class IssueReportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository(application)

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _successMessage = mutableStateOf<String?>(null)
    val successMessage: State<String?> = _successMessage

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    fun reportIssue(description: String, profileCode: String? = null, locationLabel: String? = null) {
        if (description.isBlank()) {
            _error.value = "Opis nie może być pusty"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _successMessage.value = null

            repository.reportIssue(description, profileCode, locationLabel)
                .onSuccess {
                    _successMessage.value = "Zgłoszenie wysłane pomyślnie"
                }
                .onFailure {
                    _error.value = "Błąd wysyłania zgłoszenia: ${it.message}"
                }

            _isLoading.value = false
        }
    }

    fun clearMessages() {
        _successMessage.value = null
        _error.value = null
    }
}
