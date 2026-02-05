package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.NetworkModule
import com.example.warehouse.data.local.SettingsDataStore
import com.example.warehouse.data.model.CutPlanResponse
import com.example.warehouse.data.model.OptimizationRequest
import com.example.warehouse.data.repository.InventoryRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OptimizationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = InventoryRepository(application)
    private val settingsDataStore = SettingsDataStore(application)

    private val _result = mutableStateOf<CutPlanResponse?>(null)
    val result: State<CutPlanResponse?> = _result

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _profiles = mutableStateOf<List<String>>(emptyList())
    val profiles: State<List<String>> = _profiles

    private val _colors = mutableStateOf<List<String>>(emptyList())
    val colors: State<List<String>> = _colors

    private var reservedWasteLengths: List<Int> = emptyList()

    init {
        viewModelScope.launch {
            repository.getProfilesFlow().collectLatest { entities ->
                _profiles.value = entities.map { it.code }
            }
        }
        viewModelScope.launch {
            repository.getColorsFlow().collectLatest { entities ->
                _colors.value = entities.map { it.code }
            }
        }
        viewModelScope.launch {
            settingsDataStore.reservedWasteLengths.collectLatest { csv ->
                reservedWasteLengths = csv.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
            }
        }
    }

    fun calculate(
        profile: String, 
        internalColor: String, 
        externalColor: String, 
        coreColor: String?, 
        pieces: List<Int>
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _result.value = null
            try {
                val request = OptimizationRequest(
                    profileCode = profile,
                    internalColor = internalColor,
                    externalColor = externalColor,
                    coreColor = coreColor,
                    requiredPieces = pieces,
                    preferWaste = true,
                    reserveWasteLengths = reservedWasteLengths
                )
                _result.value = NetworkModule.api.calculateOptimization(request)
            } catch (e: Exception) {
                _error.value = "Błąd: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
