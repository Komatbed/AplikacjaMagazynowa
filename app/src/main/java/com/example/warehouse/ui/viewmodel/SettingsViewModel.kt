package com.example.warehouse.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.NetworkModule
import com.example.warehouse.data.local.SettingsDataStore
import com.example.warehouse.data.local.dataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.warehouse.device.PrinterService
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State

import kotlinx.coroutines.delay
import java.util.Date

sealed class BackendStatus {
    object Unknown : BackendStatus()
    object Checking : BackendStatus()
    data class Online(val latencyMs: Long, val lastCheck: Date) : BackendStatus()
    data class Offline(val message: String, val lastCheck: Date) : BackendStatus()
}

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsDataStore = SettingsDataStore(application)
    private val printerService = PrinterService()

    private val _printerStatus = mutableStateOf<String?>(null)
    val printerStatus: State<String?> = _printerStatus

    private val _backendStatus = mutableStateOf<BackendStatus>(BackendStatus.Unknown)
    val backendStatus: State<BackendStatus> = _backendStatus

    init {
        startPeriodicCheck()
    }

    private fun startPeriodicCheck() {
        viewModelScope.launch {
            while (true) {
                checkBackendConnection()
                delay(30000) // Check every 30 seconds
            }
        }
    }

    val apiUrl = settingsDataStore.apiUrl.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "http://192.168.1.101:8080/api/v1/"
    )

    val printerIp = settingsDataStore.printerIp.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "192.168.1.100"
    )

    val printerPort = settingsDataStore.printerPort.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        9100
    )
    
    val scrapThreshold = settingsDataStore.scrapThreshold.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        500
    )
    
    val reservedWasteLengths = settingsDataStore.reservedWasteLengths.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    fun saveSettings(url: String, ip: String, port: String, threshold: String, reservedLengths: String) {
        viewModelScope.launch {
            settingsDataStore.saveApiUrl(url)
            settingsDataStore.savePrinterIp(ip)
            settingsDataStore.saveReservedWasteLengths(reservedLengths)
            
            port.toIntOrNull()?.let { 
                settingsDataStore.savePrinterPort(it)
            }
            
            threshold.toIntOrNull()?.let {
                settingsDataStore.saveScrapThreshold(it)
            }

            // Update Network Module immediately
            NetworkModule.updateUrl(url)
            checkBackendConnection()
        }
    }

    fun checkBackendConnection() {
        viewModelScope.launch {
            _backendStatus.value = BackendStatus.Checking
            val start = System.currentTimeMillis()
            try {
                // Use a lightweight call to check connection
                NetworkModule.api.getConfig() // or getProfiles()
                val latency = System.currentTimeMillis() - start
                _backendStatus.value = BackendStatus.Online(latency, Date())
            } catch (e: Exception) {
                _backendStatus.value = BackendStatus.Offline(e.message ?: "Błąd połączenia", Date())
            }
        }
    }

    fun testPrinterConnection(ip: String, port: String) {
        val portInt = port.toIntOrNull() ?: 9100
        viewModelScope.launch {
            _printerStatus.value = "Łączenie..."
            val result = printerService.testConnection(ip, portInt)
            _printerStatus.value = result.getOrElse { it.message }
        }
    }

    fun printTestLabel(ip: String, port: String) {
        val portInt = port.toIntOrNull() ?: 9100
        viewModelScope.launch {
            _printerStatus.value = "Drukowanie..."
            val result = printerService.printTestLabel(ip, portInt)
            _printerStatus.value = result.getOrElse { it.message }
        }
    }
}
