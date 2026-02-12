package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.local.WarehouseDatabase
import com.example.warehouse.data.local.SettingsDataStore
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
    private val settingsDataStore = SettingsDataStore(application)
    private val presetDao = db.presetDao()

    // Data Sources
    val profiles: StateFlow<List<ProfileEntity>> = MutableStateFlow(emptyList()) // Placeholder, ideally from DAO flow
    val colors: StateFlow<List<ColorEntity>> = MutableStateFlow(emptyList())
    val presets = presetDao.getAllPresets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Custom Multi-Core Colors (loaded from Settings/DataStore)
    // These are colors that allow core selection (e.g. Golden Oak, Walnut)
    private val _customMultiCoreColors = MutableStateFlow<Set<String>>(emptySet())
    val customMultiCoreColors: StateFlow<Set<String>> = _customMultiCoreColors

    // Selection State
    private val _selectedProfile = MutableStateFlow<ProfileEntity?>(null)
    val selectedProfile: StateFlow<ProfileEntity?> = _selectedProfile

    private val _selectedExternalColor = MutableStateFlow<ColorEntity?>(null)
    val selectedExternalColor: StateFlow<ColorEntity?> = _selectedExternalColor

    private val _internalColorMode = MutableStateFlow(InternalColorMode.SAME_AS_EXTERNAL)
    val internalColorMode: StateFlow<InternalColorMode> = _internalColorMode

    private val _calculatedCoreColor = MutableStateFlow("white")
    val calculatedCoreColor: StateFlow<String> = _calculatedCoreColor

    private val _availableCoreColors = MutableStateFlow<List<String>>(emptyList())
    val availableCoreColors: StateFlow<List<String>> = _availableCoreColors

    private val _isCoreSelectionEnabled = MutableStateFlow(false)
    val isCoreSelectionEnabled: StateFlow<Boolean> = _isCoreSelectionEnabled

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
            settingsDataStore.customMultiCoreColors.collect { csv ->
                val list = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                _customMultiCoreColors.value = list
                recalculateCoreColor()
            }
        }
        viewModelScope.launch {
            configRepository.getCoreColorRulesFlow().collect { rules ->
                coreColorRules = rules.associate { it.extColorCode to it.coreColorCode }
                
                // Extract unique core colors for the dropdown
                val cores = rules.map { it.coreColorCode }.distinct().sorted().toMutableList()
                // Ensure basic cores are present if not in rules
                val basicCores = listOf("white", "brown", "caramel", "anthracite", "grey", "black")
                basicCores.forEach { if (!cores.contains(it)) cores.add(it) }
                
                _availableCoreColors.value = cores
                
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
        } else if (preset.internalColor.equals("RAL 9001", ignoreCase = true)) {
            setInternalColorMode(InternalColorMode.RAL_9001)
        } else if (preset.internalColor == preset.externalColor) {
            setInternalColorMode(InternalColorMode.SAME_AS_EXTERNAL)
        }
    }

    fun savePreset(name: String) {
        val p = _selectedProfile.value ?: return
        val c = _selectedExternalColor.value ?: return
        
        val intColor = when (_internalColorMode.value) {
            InternalColorMode.WHITE -> "white"
            InternalColorMode.RAL_9001 -> "RAL 9001"
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

    fun setCoreColor(color: String) {
        _calculatedCoreColor.value = color
    }
    
    fun updateCustomMultiCoreColors(newColors: Set<String>) {
        _customMultiCoreColors.value = newColors
        recalculateCoreColor()
    }

    private fun recalculateCoreColor() {
        val extEntity = _selectedExternalColor.value ?: return
        val extCode = extEntity.code
        val extName = extEntity.name
        
        val intCode = when (_internalColorMode.value) {
            InternalColorMode.WHITE -> "white"
            InternalColorMode.RAL_9001 -> "RAL 9001"
            InternalColorMode.SAME_AS_EXTERNAL -> extCode
        }

        val core = CoreColorCalculator.calculate(extCode, intCode, coreColorRules)
        _calculatedCoreColor.value = core

        // Logic based on User Requirement:
        // "I want to manually define a list of colors that can have active core selection (X/X, X/Y types)"
        
        // Check if the external color matches any of the manually defined multi-core colors
        val isManualMultiCore = _customMultiCoreColors.value.any { known -> 
            extName.contains(known, ignoreCase = true) || extCode.contains(known, ignoreCase = true)
        }
        
        // Also enable if it's a true bi-color (different foils on both sides, neither is white)
        // Note: The user said "X/Y", implying mixed foils.
        val isTrueBiColor = !intCode.equals(extCode, ignoreCase = true) && 
                            !isWhiteOrCream(intCode) && 
                            !isWhiteOrCream(extCode)

        // Enable selection if it's in the manual list OR it's a true mixed-foil configuration
        // AND neither side is white (which enforces one-sided/white core)
        val isNotWhiteOneSided = !isWhiteOrCream(extCode) && !isWhiteOrCream(intCode)

        // Special case for RAL 9001 internal mode:
        // If Internal is RAL 9001, it acts like White (one-sided foil), so usually Core is fixed (White/Cream).
        // Unless specific profile rules say otherwise, we generally treat 9001 as "White-ish" for core blocking.
        // User requested: "RAL9001 - of course this will appear/activate only for selected group of colors".
        // This likely means RAL 9001 button should only appear for certain colors, OR core selection with 9001?
        // Assuming user meant the RAL 9001 OPTION is available. Core selection logic remains: if one side is 9001, core is fixed.
        
        _isCoreSelectionEnabled.value = (isManualMultiCore || isTrueBiColor) && isNotWhiteOneSided
    }

    private fun isWhiteOrCream(codeOrName: String): Boolean {
        val c = codeOrName.lowercase()
        return c.contains("white") || 
               c.contains("biały") || 
               c.contains("9016") || 
               c.contains("9001") ||
               c.contains("krem") ||
               c.contains("cream") ||
               c.contains("papirus")
    }

    enum class InternalColorMode {
        WHITE, RAL_9001, SAME_AS_EXTERNAL
    }
}