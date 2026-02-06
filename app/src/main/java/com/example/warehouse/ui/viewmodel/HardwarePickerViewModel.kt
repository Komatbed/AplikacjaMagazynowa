package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.local.WarehouseDatabase
import com.example.warehouse.data.local.entity.PresetEntity
import com.example.warehouse.data.local.entity.ProfileEntity
import com.example.warehouse.util.FittingComponent
import com.example.warehouse.util.FittingSystem
import com.example.warehouse.util.HardwareCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn

class HardwarePickerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = WarehouseDatabase.getDatabase(application)
    private val presetDao = database.presetDao()
    private val configDao = database.configDao()

    private val _components = MutableStateFlow<List<FittingComponent>>(emptyList())
    val components: StateFlow<List<FittingComponent>> = _components.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Presets & Profiles
    val presets = presetDao.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    val profiles = configDao.getProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProfile = MutableStateFlow<ProfileEntity?>(null)
    val selectedProfile: StateFlow<ProfileEntity?> = _selectedProfile.asStateFlow()

    fun selectProfile(profile: ProfileEntity?) {
        _selectedProfile.value = profile
    }

    fun loadPreset(preset: PresetEntity) {
        // Find profile by code
        val profile = profiles.value.find { it.code == preset.profileCode }
        if (profile != null) {
            _selectedProfile.value = profile
        }
    }

    fun calculate(system: FittingSystem, ffb: String, ffh: String) {
        _error.value = null
        val w = ffb.toIntOrNull()
        val h = ffh.toIntOrNull()

        if (w == null || h == null) {
            _error.value = "Wprowadź poprawne wymiary (liczby całkowite mm)"
            return
        }

        if (w < 260 || h < 260) {
            _error.value = "Wymiary poza zakresem (min 260mm)"
            return
        }

        val result = HardwareCalculator.calculate(
            system = system, 
            ffb = w, 
            ffh = h,
            profileCode = _selectedProfile.value?.code
        )
        _components.value = result
    }

    fun clear() {
        _components.value = emptyList()
        _error.value = null
        _selectedProfile.value = null
    }
}
