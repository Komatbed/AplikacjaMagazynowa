package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.local.WarehouseDatabase
import com.example.warehouse.data.local.dao.ConfigDao
import com.example.warehouse.data.local.entity.ColorEntity
import com.example.warehouse.data.local.entity.ProfileEntity
import com.example.warehouse.data.repository.ConfigRepository
import com.example.warehouse.util.CoreColorCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.example.warehouse.data.local.entity.PresetEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AddInventoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = WarehouseDatabase.getDatabase(application)
    private val configRepository = ConfigRepository(application)
    private val presetDao = db.presetDao()

    // Data Sources
    val profiles: StateFlow<List<ProfileEntity>> = MutableStateFlow(emptyList()) // Placeholder, ideally from DAO flow
    val colors: StateFlow<List<ColorEntity>> = MutableStateFlow(emptyList())
    val presets = presetDao.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection State
    private val _selectedProfile = MutableStateFlow<ProfileEntity?>(null)
    val selectedProfile: StateFlow<ProfileEntity?> = _selectedProfile

    private val _selectedExternalColor = MutableStateFlow<ColorEntity?>(null)
    val selectedExternalColor: StateFlow<ColorEntity?> = _selectedExternalColor

    private val _internalColorMode = MutableStateFlow(InternalColorMode.SAME_AS_EXTERNAL)
    val internalColorMode: StateFlow<InternalColorMode> = _internalColorMode

    private val _calculatedCoreColor = MutableStateFlow("white")
    val calculatedCoreColor: StateFlow<String> = _calculatedCoreColor

    // Rules Cache
    private var coreColorRules: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch {
            configRepository.getProfilesFlow().collect { (profiles as MutableStateFlow).value = it }
        }
        viewModelScope.launch {
            configRepository.getColorsFlow().collect { (colors as MutableStateFlow).value = it }
        }
        viewModelScope.launch {
            configRepository.getCoreColorRulesFlow().collect { rules ->
                coreColorRules = rules.associate { it.extColorCode to it.coreColorCode }
                recalculateCoreColor()
            }
        }
    }

    fun loadPreset(preset: PresetEntity) {
        val profile = profiles.value.find { it.code == preset.profileCode }
        if (profile != null) selectProfile(profile)

        val color = colors.value.find { it.code == preset.externalColor }
        if (color != null) selectExternalColor(color)

        if (preset.internalColor.equals("white", ignoreCase = true)) {
            setInternalColorMode(InternalColorMode.WHITE)
        } else if (preset.internalColor == preset.externalColor) {
            setInternalColorMode(InternalColorMode.SAME_AS_EXTERNAL)
        }
    }

    fun savePreset(name: String) {
        val p = _selectedProfile.value ?: return
        val c = _selectedExternalColor.value ?: return
        
        val intColor = when (_internalColorMode.value) {
            InternalColorMode.WHITE -> "white"
            InternalColorMode.SAME_AS_EXTERNAL -> c.code
        }

        viewModelScope.launch {
            presetDao.insert(
                PresetEntity(
                    name = name,
                    profileCode = p.code,
                    externalColor = c.code,
                    internalColor = intColor
                )
            )
        }
    }

    fun deletePreset(preset: PresetEntity) {
        viewModelScope.launch {
            presetDao.delete(preset)
        }
    }

    fun selectProfile(profile: ProfileEntity) {
        _selectedProfile.value = profile
    }

    fun selectExternalColor(color: ColorEntity) {
        _selectedExternalColor.value = color
        recalculateCoreColor()
    }

    fun setInternalColorMode(mode: InternalColorMode) {
        _internalColorMode.value = mode
        recalculateCoreColor()
    }

    private fun recalculateCoreColor() {
        val ext = _selectedExternalColor.value?.code ?: return
        val int = when (_internalColorMode.value) {
            InternalColorMode.WHITE -> "white" // Or specific code
            InternalColorMode.SAME_AS_EXTERNAL -> ext
        }

        val core = CoreColorCalculator.calculate(ext, int, coreColorRules)
        _calculatedCoreColor.value = core
    }

    enum class InternalColorMode {
        WHITE, SAME_AS_EXTERNAL
    }
}
