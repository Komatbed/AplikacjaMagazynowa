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
    private val settings = com.example.warehouse.data.local.SettingsDataStore(application)

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    val profiles = repository.getProfilesFlow()
        .catch { e -> _error.value = "Błąd bazy: ${e.message}"; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val colors = repository.getColorsFlow()
        .catch { e -> _error.value = "Błąd bazy: ${e.message}"; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val favoriteProfileCodes = settings.favoriteProfileCodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val favoriteColorCodes = settings.favoriteColorCodes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

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
    
    fun toggleFavoriteProfile(code: String) {
        viewModelScope.launch {
            val current = favoriteProfileCodes.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            if (current.contains(code)) current.remove(code) else current.add(code)
            val csv = current.joinToString(",")
            try {
                com.example.warehouse.data.NetworkModule.api.updateUserPreferences(
                    com.example.warehouse.data.model.UserPreferencesDto(
                        favoriteProfileCodes = csv,
                        favoriteColorCodes = favoriteColorCodes.value,
                        preferredProfileOrder = settings.preferredProfileOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "").value,
                        preferredColorOrder = settings.preferredColorOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "").value
                    )
                )
            } catch (_: Exception) { }
            settings.saveFavoriteProfiles(csv)
        }
    }
    
    fun toggleFavoriteColor(code: String) {
        viewModelScope.launch {
            val current = favoriteColorCodes.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
            if (current.contains(code)) current.remove(code) else current.add(code)
            val csv = current.joinToString(",")
            try {
                com.example.warehouse.data.NetworkModule.api.updateUserPreferences(
                    com.example.warehouse.data.model.UserPreferencesDto(
                        favoriteProfileCodes = favoriteProfileCodes.value,
                        favoriteColorCodes = csv,
                        preferredProfileOrder = settings.preferredProfileOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "").value,
                        preferredColorOrder = settings.preferredColorOrder.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "").value
                    )
                )
            } catch (_: Exception) { }
            settings.saveFavoriteColors(csv)
        }
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

    fun importFromAssets() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val assets = getApplication<Application>().assets
                val gson = com.google.gson.Gson()

                // Read Profiles (sanitize null/blank fields, especially 'type')
                val profilesJson = assets.open("initial_data/profiles.json").bufferedReader().use { it.readText() }
                val profilesArray = com.google.gson.JsonParser.parseString(profilesJson).asJsonArray
                val profiles: List<com.example.warehouse.data.model.ProfileDefinition> = profilesArray.map { el ->
                    val obj = el.asJsonObject
                    val id = obj.get("id")?.takeUnless { it.isJsonNull }?.asString
                    val code = obj.get("code")?.takeUnless { it.isJsonNull }?.asString ?: ""
                    val description = obj.get("description")?.takeUnless { it.isJsonNull }?.asString ?: ""
                    val heightMm = obj.get("heightMm")?.takeUnless { it.isJsonNull }?.asInt ?: 0
                    val widthMm = obj.get("widthMm")?.takeUnless { it.isJsonNull }?.asInt ?: 0
                    val beadHeightMm = obj.get("beadHeightMm")?.takeUnless { it.isJsonNull }?.asInt ?: 0
                    val beadAngle = obj.get("beadAngle")?.takeUnless { it.isJsonNull }?.asDouble ?: 0.0
                    val standardLengthMm = obj.get("standardLengthMm")?.takeUnless { it.isJsonNull }?.asInt ?: 6500
                    val system = obj.get("system")?.takeUnless { it.isJsonNull }?.asString ?: ""
                    val manufacturer = obj.get("manufacturer")?.takeUnless { it.isJsonNull }?.asString ?: ""
                    val rawType = obj.get("type")?.takeUnless { it.isJsonNull }?.asString
                    val type = if (rawType == null || rawType.isBlank()) "OTHER" else rawType
                    com.example.warehouse.data.model.ProfileDefinition(
                        id = id,
                        code = code,
                        description = description,
                        heightMm = heightMm,
                        widthMm = widthMm,
                        beadHeightMm = beadHeightMm,
                        beadAngle = beadAngle,
                        standardLengthMm = standardLengthMm,
                        system = system,
                        manufacturer = manufacturer,
                        type = type
                    )
                }

                // Read Colors (keep Gson defaults; sanitize type if null/blank)
                val colorsJson = assets.open("initial_data/colors.json").bufferedReader().use { it.readText() }
                val colorsArray = com.google.gson.JsonParser.parseString(colorsJson).asJsonArray
                val colors: List<com.example.warehouse.data.model.ColorDefinition> = colorsArray.map { el ->
                    val obj = el.asJsonObject
                    val id = obj.get("id")?.takeUnless { it.isJsonNull }?.asString
                    val code = obj.get("code")?.takeUnless { it.isJsonNull }?.asString ?: ""
                    val description = obj.get("description")?.takeUnless { it.isJsonNull }?.asString ?: ""
                    val name = obj.get("name")?.takeUnless { it.isJsonNull }?.asString ?: ""
                    val paletteCode = obj.get("paletteCode")?.takeUnless { it.isJsonNull }?.asString ?: ""
                    val vekaCode = obj.get("vekaCode")?.takeUnless { it.isJsonNull }?.asString ?: ""
                    val rawType = obj.get("type")?.takeUnless { it.isJsonNull }?.asString
                    val type = if (rawType == null || rawType.isBlank()) "smooth" else rawType
                    val foilManufacturer = obj.get("foilManufacturer")?.takeUnless { it.isJsonNull }?.asString ?: ""
                    com.example.warehouse.data.model.ColorDefinition(
                        id = id,
                        code = code,
                        description = description,
                        name = name,
                        paletteCode = paletteCode,
                        vekaCode = vekaCode,
                        type = type,
                        foilManufacturer = foilManufacturer
                    )
                }

                val result = repository.saveInitialData(profiles, colors)
                result.onFailure { _error.value = "Błąd importu: ${it.message}" }
                result.onSuccess { 
                    // No need to explicit refresh as flows are observed, but we can trigger one just in case
                }

            } catch (e: Exception) {
                _error.value = "Nie udało się wczytać plików z assets: ${e.message}"
            }
            _isLoading.value = false
        }
    }
}
