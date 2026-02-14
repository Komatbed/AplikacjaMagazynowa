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
    private val inventoryRepository = com.example.warehouse.data.repository.InventoryRepository(application)
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
    
    // Preferred orders / favorites
    private val preferredProfileOrderCodes = MutableStateFlow<List<String>>(emptyList())
    private val preferredColorOrderCodes = MutableStateFlow<List<String>>(emptyList())
    private val favoriteProfileCodes = MutableStateFlow<Set<String>>(emptySet())
    private val favoriteColorCodes = MutableStateFlow<Set<String>>(emptySet())

    // Rules Cache
    private var coreColorRules: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch {
            configRepository.getProfilesFlow().collect { list ->
                val ordered = applyPreferredOrderToProfiles(list, preferredProfileOrderCodes.value, favoriteProfileCodes.value)
                (profiles as MutableStateFlow).value = ordered
            }
        }
        viewModelScope.launch {
            configRepository.getColorsFlow().collect { list ->
                val ordered = applyPreferredOrderToColors(list, preferredColorOrderCodes.value, favoriteColorCodes.value)
                (colors as MutableStateFlow).value = ordered
            }
        }
        viewModelScope.launch {
            settingsDataStore.customMultiCoreColors.collect { csv ->
                val list = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                _customMultiCoreColors.value = list
                recalculateCoreColor()
            }
        }
        viewModelScope.launch {
            settingsDataStore.preferredProfileOrder.collect { csv ->
                preferredProfileOrderCodes.value = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                // Re-apply ordering with current profiles
                (profiles as MutableStateFlow).value = applyPreferredOrderToProfiles(profiles.value, preferredProfileOrderCodes.value, favoriteProfileCodes.value)
            }
        }
        viewModelScope.launch {
            settingsDataStore.preferredColorOrder.collect { csv ->
                preferredColorOrderCodes.value = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                (colors as MutableStateFlow).value = applyPreferredOrderToColors(colors.value, preferredColorOrderCodes.value, favoriteColorCodes.value)
            }
        }
        viewModelScope.launch {
            settingsDataStore.favoriteProfileCodes.collect { csv ->
                favoriteProfileCodes.value = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                (profiles as MutableStateFlow).value = applyPreferredOrderToProfiles(profiles.value, preferredProfileOrderCodes.value, favoriteProfileCodes.value)
            }
        }
        viewModelScope.launch {
            settingsDataStore.favoriteColorCodes.collect { csv ->
                favoriteColorCodes.value = csv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                (colors as MutableStateFlow).value = applyPreferredOrderToColors(colors.value, preferredColorOrderCodes.value, favoriteColorCodes.value)
            }
        }
        viewModelScope.launch {
            configRepository.getCoreColorRulesFlow().collect { rules ->
                // Use ext code -> core display name mapping, translated
                coreColorRules = rules.associate { it.extColorCode to it.coreColorCode }
                
                // Extract unique core colors for the dropdown
                val cores = rules.map { it.coreColorCode }.distinct().sorted().toMutableList()
                // Ensure basic cores are present if not in rules
                val basicCores = listOf("biały", "brąz", "karmel", "antracyt", "szary", "czarny", "kremowy")
                basicCores.forEach { if (!cores.contains(it)) cores.add(it) }
                
                // Translate possible English codes to PL for the dropdown
                _availableCoreColors.value = cores.map { translateCoreToPL(it) }
                
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
            InternalColorMode.WHITE -> "biały"
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
    
    private fun translateCoreToPL(code: String): String {
        return when (code.lowercase()) {
            "white", "biały", "bialy", "9016" -> "biały"
            "brown", "brąz", "braz" -> "brąz"
            "caramel", "karmel" -> "karmel"
            "anthracite", "antracyt" -> "antracyt"
            "grey", "gray", "szary" -> "szary"
            "black", "czarny" -> "czarny"
            "cream", "krem", "kremowy", "9001" -> "kremowy"
            else -> code
        }
    }
    
    private fun applyPreferredOrderToProfiles(list: List<ProfileEntity>, order: List<String>, favorites: Set<String>): List<ProfileEntity> {
        val orderIndex = order.withIndex().associate { it.value to it.index }
        return list.sortedWith(compareBy(
            { if (favorites.contains(it.code)) 0 else 1 },
            { orderIndex[it.code] ?: Int.MAX_VALUE },
            { it.code.lowercase() }
        ))
    }
    
    private fun applyPreferredOrderToColors(list: List<ColorEntity>, order: List<String>, favorites: Set<String>): List<ColorEntity> {
        val orderIndex = order.withIndex().associate { it.value to it.index }
        return list.sortedWith(compareBy(
            { if (favorites.contains(it.code)) 0 else 1 },
            { orderIndex[it.code] ?: Int.MAX_VALUE },
            { (it.name.ifBlank { it.code }).lowercase() }
        ))
    }
    
    fun addToInventory(locationLabel: String, lengthMm: Int, quantity: Int) {
        val profile = _selectedProfile.value ?: return
        val external = _selectedExternalColor.value ?: return
        val internal = when (_internalColorMode.value) {
            InternalColorMode.WHITE -> "biały"
            InternalColorMode.RAL_9001 -> "RAL 9001"
            InternalColorMode.SAME_AS_EXTERNAL -> external.code
        }
        val core = _calculatedCoreColor.value
        viewModelScope.launch {
            inventoryRepository.addItem(
                locationLabel = locationLabel,
                profileCode = profile.code,
                internalColor = internal,
                externalColor = external.code,
                coreColor = if (_isCoreSelectionEnabled.value) core else core, // core is auto-PL
                lengthMm = lengthMm,
                quantity = quantity,
                status = "AVAILABLE"
            )
        }
    }

    enum class InternalColorMode {
        WHITE, RAL_9001, SAME_AS_EXTERNAL
    }
}
