package com.example.warehouse.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val API_URL = stringPreferencesKey("api_url")
        val PRINTER_IP = stringPreferencesKey("printer_ip")
        val PRINTER_PORT = intPreferencesKey("printer_port")
        val SCRAP_THRESHOLD = intPreferencesKey("scrap_threshold") // in mm
        val PREFER_WASTE_RESERVATION = androidx.datastore.preferences.core.booleanPreferencesKey("prefer_waste_reservation")
        val RESERVED_WASTE_LENGTHS = stringPreferencesKey("reserved_waste_lengths") // Comma separated
        val CUSTOM_MULTI_CORE_COLORS = stringPreferencesKey("custom_multi_core_colors") // Comma separated
        val RAL9001_ELIGIBLE_COLORS = stringPreferencesKey("ral9001_eligible_colors") // Comma separated
        val PREFERRED_PROFILE_ORDER = stringPreferencesKey("preferred_profile_order") // CSV of profile codes
        val PREFERRED_COLOR_ORDER = stringPreferencesKey("preferred_color_order") // CSV of color codes
        val FAVORITE_PROFILE_CODES = stringPreferencesKey("favorite_profile_codes") // CSV of codes
        val FAVORITE_COLOR_CODES = stringPreferencesKey("favorite_color_codes") // CSV of codes
        val REMEMBERED_USERNAME = stringPreferencesKey("remembered_username")
        val SKIP_LOGIN = androidx.datastore.preferences.core.booleanPreferencesKey("skip_login")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val MUNTIN_MANUAL_INPUT = androidx.datastore.preferences.core.booleanPreferencesKey("muntin_manual_input")
    }

    val apiUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[API_URL] ?: "https://51.77.59.105/api/v1/"
    }

    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN]
    }

    val rememberedUsername: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[REMEMBERED_USERNAME]
    }

    val skipLogin: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SKIP_LOGIN] ?: false
    }

    val preferWasteReservation: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PREFER_WASTE_RESERVATION] ?: false
    }
    
    val reservedWasteLengths: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[RESERVED_WASTE_LENGTHS] ?: ""
    }

    val customMultiCoreColors: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CUSTOM_MULTI_CORE_COLORS] ?: "Złoty Dąb, Orzech, Winchester XA, Dąb Bagienny, Machoń, Daglezja, Sosna Górska, Oregon III, Dąb Rustykalny, Bazaltowo-szary, Ciemnoszary, Antracytowy Ultramatowy, Dąb Sheffield Jasny, Dąb Sheffield Szary, Kwarcowo-szary, Dąb Klejony Miodowy Super Mat, Dąb Klejony Coriander Super Mat, Dąb Klejony Pieprzowy Super Mat"
    }
    
    val ral9001EligibleColors: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[RAL9001_ELIGIBLE_COLORS] ?: ""
    }
    
    val preferredProfileOrder: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREFERRED_PROFILE_ORDER] ?: ""
    }
    
    val preferredColorOrder: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PREFERRED_COLOR_ORDER] ?: ""
    }
    
    val favoriteProfileCodes: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_PROFILE_CODES] ?: ""
    }
    
    val favoriteColorCodes: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[FAVORITE_COLOR_CODES] ?: ""
    }

    val printerIp: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PRINTER_IP] ?: "192.168.1.100"
    }

    val printerPort: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PRINTER_PORT] ?: 9100
    }
    
    val scrapThreshold: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SCRAP_THRESHOLD] ?: 500
    }
    
    val muntinManualInput: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MUNTIN_MANUAL_INPUT] ?: false
    }

    suspend fun saveApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[API_URL] = url
        }
    }

    suspend fun savePrinterIp(ip: String) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_IP] = ip
        }
    }

    suspend fun savePrinterPort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[PRINTER_PORT] = port
        }
    }
    
    suspend fun saveScrapThreshold(threshold: Int) {
        context.dataStore.edit { preferences ->
            preferences[SCRAP_THRESHOLD] = threshold
        }
    }
    
    suspend fun saveMuntinManualInput(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MUNTIN_MANUAL_INPUT] = enabled
        }
    }

    suspend fun savePreferWasteReservation(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PREFER_WASTE_RESERVATION] = enabled
        }
    }
    
    suspend fun saveReservedWasteLengths(lengths: String) {
        context.dataStore.edit { preferences ->
            preferences[RESERVED_WASTE_LENGTHS] = lengths
        }
    }

    suspend fun saveCustomMultiCoreColors(colors: String) {
        context.dataStore.edit { preferences ->
            preferences[CUSTOM_MULTI_CORE_COLORS] = colors
        }
    }

    suspend fun saveRal9001EligibleColors(colors: String) {
        context.dataStore.edit { preferences ->
            preferences[RAL9001_ELIGIBLE_COLORS] = colors
        }
    }
    
    suspend fun savePreferredProfileOrder(csv: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_PROFILE_ORDER] = csv
        }
    }
    
    suspend fun savePreferredColorOrder(csv: String) {
        context.dataStore.edit { preferences ->
            preferences[PREFERRED_COLOR_ORDER] = csv
        }
    }
    
    suspend fun saveFavoriteProfiles(csv: String) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_PROFILE_CODES] = csv
        }
    }
    
    suspend fun saveFavoriteColors(csv: String) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_COLOR_CODES] = csv
        }
    }

    suspend fun saveRememberedUsername(username: String?) {
        context.dataStore.edit { preferences ->
            if (username != null) {
                preferences[REMEMBERED_USERNAME] = username
            } else {
                preferences.remove(REMEMBERED_USERNAME)
            }
        }
    }

    suspend fun saveAuthToken(token: String?) {
        context.dataStore.edit { preferences ->
            if (token != null) {
                preferences[AUTH_TOKEN] = token
            } else {
                preferences.remove(AUTH_TOKEN)
            }
        }
    }

    suspend fun saveSkipLogin(skip: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SKIP_LOGIN] = skip
        }
    }
}
