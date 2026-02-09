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
import com.example.warehouse.data.repository.InventoryRepository
import com.example.warehouse.device.BluetoothPrinterManager
import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import java.net.HttpURLConnection
import java.net.URL

sealed class BackendStatus {
    object Unknown : BackendStatus()
    object Checking : BackendStatus()
    data class Online(val latencyMs: Long, val lastCheck: Date) : BackendStatus()
    data class Offline(val message: String, val lastCheck: Date) : BackendStatus()
}

class SettingsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: InventoryRepository = InventoryRepository(application)
) : AndroidViewModel(application) {
    private val settingsDataStore = SettingsDataStore(application)
    private val printerService = PrinterService()
    val bluetoothPrinterManager = BluetoothPrinterManager(application)

    private val _printerStatus = mutableStateOf<String?>(null)
    val printerStatus: State<String?> = _printerStatus

    private val _backendStatus = mutableStateOf<BackendStatus>(BackendStatus.Unknown)
    val backendStatus: State<BackendStatus> = _backendStatus

    private val _dbStatus = mutableStateOf<BackendStatus>(BackendStatus.Unknown)
    val dbStatus: State<BackendStatus> = _dbStatus

    private val _webStatus = mutableStateOf<BackendStatus>(BackendStatus.Unknown)
    val webStatus: State<BackendStatus> = _webStatus

    // Changed to 5 seconds per requirements
    private fun startPeriodicCheck() {
        viewModelScope.launch {
            while (true) {
                checkAllConnections()
                delay(5000) 
            }
        }
    }

    private fun checkAllConnections() {
        checkBackendConnection()
        checkDbConnection()
        checkWebConnection()
    }

    fun connectBluetoothPrinter(device: BluetoothDevice) {
        viewModelScope.launch {
            bluetoothPrinterManager.connect(device)
        }
    }

    val apiUrl = settingsDataStore.apiUrl.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "https://51.77.59.105/api/v1/"
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

    val skipLogin = settingsDataStore.skipLogin.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    fun saveSkipLogin(skip: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveSkipLogin(skip)
        }
    }

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
                withTimeout(10000) {
                    // Use repository to check connection
                    val result = repository.checkConnection()
                    if (result.isSuccess) {
                        val latency = System.currentTimeMillis() - start
                        _backendStatus.value = BackendStatus.Online(latency, Date())
                    } else {
                        _backendStatus.value = BackendStatus.Offline(result.exceptionOrNull()?.message ?: "Błąd połączenia", Date())
                    }
                }
            } catch (e: Exception) {
                _backendStatus.value = BackendStatus.Offline(e.message ?: "Timeout/Błąd", Date())
            }
        }
    }

    private fun checkDbConnection() {
        // Assuming DB is OK if Backend is OK (for now, as we don't have separate DB health endpoint)
        // In a real scenario, we would call a specific endpoint like /actuator/health/db
        viewModelScope.launch {
             _dbStatus.value = _backendStatus.value
        }
    }

    private fun checkWebConnection() {
        val currentApiUrl = apiUrl.value
        val webUrl = try {
            val urlObj = URL(currentApiUrl)
            "${urlObj.protocol}://${urlObj.host}"
        } catch (e: Exception) {
            "http://51.77.59.105"
        }

        viewModelScope.launch {
            _webStatus.value = BackendStatus.Checking
            val start = System.currentTimeMillis()
            
            try {
                // Perform network request on IO dispatcher
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    try {
                        withTimeout(10000) {
                            val url = URL(webUrl)
                            val connection = url.openConnection() as HttpURLConnection
                            connection.connectTimeout = 5000
                            connection.readTimeout = 5000
                            connection.requestMethod = "HEAD"
                            
                            val code = connection.responseCode
                            connection.disconnect()
                            
                            if (code in 200..399) {
                                Result.success(code)
                            } else {
                                Result.failure(Exception("HTTP $code"))
                            }
                        }
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
                
                // Update state on Main dispatcher (default for viewModelScope)
                result.onSuccess {
                    val latency = System.currentTimeMillis() - start
                    _webStatus.value = BackendStatus.Online(latency, Date())
                }.onFailure { e ->
                    _webStatus.value = BackendStatus.Offline(e.message ?: "Web Unreachable", Date())
                }
            } catch (e: Exception) {
                _webStatus.value = BackendStatus.Offline(e.message ?: "Unexpected Error", Date())
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

    init {
        startPeriodicCheck()
    }
}
