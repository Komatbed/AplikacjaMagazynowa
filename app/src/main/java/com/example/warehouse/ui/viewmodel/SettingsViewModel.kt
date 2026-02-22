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
import com.example.warehouse.data.repository.ConfigRepository
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
    private val repository: InventoryRepository = InventoryRepository(application),
    private val printerService: PrinterService = PrinterService(),
    private val configRepository: ConfigRepository = ConfigRepository(application),
    val bluetoothPrinterManager: BluetoothPrinterManager = BluetoothPrinterManager(application)
) : AndroidViewModel(application) {
    private val settingsDataStore = SettingsDataStore(application)

    private var lastConfigSync: Long = 0L

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

    private fun syncWarehouseConfig() {
        viewModelScope.launch {
            configRepository.getWarehouseConfig().onSuccess { config ->
                // Update local settings if they differ or just overwrite
                // Note: This overrides local changes every 5 seconds if backend is reachable.
                // In a real app, we might want a version check or "last updated" timestamp.
                // For now, backend is truth.
                
                (config["customMultiCoreColors"] as? List<*>)?.let { list ->
                    val colorsStr = list.filterIsInstance<String>().joinToString(",")
                    if (colorsStr != customMultiCoreColors.value) {
                        settingsDataStore.saveCustomMultiCoreColors(colorsStr)
                    }
                }
                
                (config["ral9001EligibleColors"] as? List<*>)?.let { list ->
                    val colorsStr = list.filterIsInstance<String>().joinToString(",")
                    settingsDataStore.saveRal9001EligibleColors(colorsStr)
                }
                
                (config["lowStockThreshold"] as? Number)?.toInt()?.let {
                    if (it != scrapThreshold.value) { // Assuming scrapThreshold matches lowStock? No, wait.
                        // lowStockThreshold in backend seems to be generic. 
                        // scrapThreshold in Android is for "waste" length? No, scrapThreshold is "Próg odpadu".
                        // lowStockThreshold is "Próg ostrzegania o niskim stanie".
                        // Android app doesn't seem to have "lowStockThreshold" in SettingsDataStore visible here.
                        // Ah, scrapThreshold is likely for determining if a piece is scrap or usable.
                        // Let's stick to customMultiCoreColors for now.
                    }
                }
                
                (config["reserveWasteLengths"] as? List<*>)?.let { list ->
                     val lengthsStr = list.filterIsInstance<Int>().joinToString(",")
                     if (lengthsStr != reservedWasteLengths.value) {
                         settingsDataStore.saveReservedWasteLengths(lengthsStr)
                     }
                }
            }
        }
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
    
    val customMultiCoreColors = settingsDataStore.customMultiCoreColors.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ""
    )

    val skipLogin = settingsDataStore.skipLogin.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )
    
    val muntinManualInput = settingsDataStore.muntinManualInput.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false
    )

    init {
        startPeriodicCheck()
        
        // Monitor Settings changes to update NetworkModule
        viewModelScope.launch {
            settingsDataStore.apiUrl.collect { url ->
                NetworkModule.updateUrl(url)
            }
        }
        
        viewModelScope.launch {
            settingsDataStore.authToken.collect { token ->
                NetworkModule.authToken = token
            }
        }
    }

    fun saveSkipLogin(skip: Boolean) {
        viewModelScope.launch {
            settingsDataStore.saveSkipLogin(skip)
        }
    }
    
    fun saveApiUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.saveApiUrl(url)
            NetworkModule.updateUrl(url)
            checkBackendConnection()
        }
    }
    
    fun savePrinterIp(ip: String) {
        viewModelScope.launch { settingsDataStore.savePrinterIp(ip) }
    }
    
    fun savePrinterPort(port: Int) {
        viewModelScope.launch { settingsDataStore.savePrinterPort(port) }
    }
    
    fun saveScrapThreshold(threshold: Int) {
        viewModelScope.launch { settingsDataStore.saveScrapThreshold(threshold) }
    }
    
    fun saveMuntinManualInput(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.saveMuntinManualInput(enabled) }
    }
    
    fun saveReservedWasteLengths(lengths: String) {
        viewModelScope.launch { settingsDataStore.saveReservedWasteLengths(lengths) }
    }

    fun saveCustomMultiCoreColors(colors: String) {
        viewModelScope.launch { settingsDataStore.saveCustomMultiCoreColors(colors) }
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
            _dbStatus.value = BackendStatus.Checking
            val start = System.currentTimeMillis()
            try {
                withTimeout(10000) {
                    // Use repository to check connection
                    val result = repository.checkConnection()
                    if (result.isSuccess) {
                        val latency = System.currentTimeMillis() - start
                        val onlineStatus = BackendStatus.Online(latency, Date())
                        _backendStatus.value = onlineStatus
                        _dbStatus.value = onlineStatus
                        syncWarehouseConfig()
                        val now = System.currentTimeMillis()
                        if (now - lastConfigSync > 10 * 60 * 1000) {
                            val cfgResult = configRepository.refreshConfig()
                            if (cfgResult.isSuccess) {
                                lastConfigSync = now
                            }
                        }
                    } else {
                        val offlineStatus = BackendStatus.Offline(result.exceptionOrNull()?.message ?: "Błąd połączenia", Date())
                        _backendStatus.value = offlineStatus
                        _dbStatus.value = offlineStatus
                    }
                }
            } catch (e: Exception) {
                val offlineStatus = BackendStatus.Offline(e.message ?: "Timeout/Błąd", Date())
                _backendStatus.value = offlineStatus
                _dbStatus.value = offlineStatus
            }
        }
    }

    private fun checkDbConnection() {
    }

    private fun checkWebConnection() {
        val currentApiUrl = apiUrl.value
        val webUrl = try {
            val urlObj = URL(currentApiUrl)
            "http://${urlObj.host}"
        } catch (e: Exception) {
            "http://51.77.59.105"
        }

        viewModelScope.launch {
            _webStatus.value = BackendStatus.Checking
            val start = System.currentTimeMillis()
            
            try {
                val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    try {
                        withTimeout(10000) {
                            val url = URL(webUrl)
                            val connection = url.openConnection() as HttpURLConnection
                            connection.instanceFollowRedirects = false
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
}
