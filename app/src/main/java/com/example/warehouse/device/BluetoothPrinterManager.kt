package com.example.warehouse.device

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.util.UUID

class BluetoothPrinterManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter

    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()

    private val _connectionStatus = MutableStateFlow<PrinterConnectionStatus>(PrinterConnectionStatus.Disconnected)
    val connectionStatus = _connectionStatus.asStateFlow()

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // SPP UUID
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    sealed class PrinterConnectionStatus {
        object Disconnected : PrinterConnectionStatus()
        object Scanning : PrinterConnectionStatus()
        object Connecting : PrinterConnectionStatus()
        data class Connected(val deviceName: String) : PrinterConnectionStatus()
        data class Error(val message: String) : PrinterConnectionStatus()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                device?.let {
                    val currentList = _scannedDevices.value.toMutableList()
                    if (currentList.none { d -> d.address == it.address }) {
                        currentList.add(it)
                        _scannedDevices.value = currentList
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter?.isEnabled == true) {
            _connectionStatus.value = PrinterConnectionStatus.Scanning
            
            // Add bonded devices first
            val bonded = bluetoothAdapter.bondedDevices.toList()
            _scannedDevices.value = bonded

            val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            context.registerReceiver(receiver, filter)
            bluetoothAdapter.startDiscovery()
        } else {
            _connectionStatus.value = PrinterConnectionStatus.Error("Bluetooth wyłączony")
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
        if (_connectionStatus.value == PrinterConnectionStatus.Scanning) {
            _connectionStatus.value = PrinterConnectionStatus.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun connect(device: BluetoothDevice) {
        stopScan()
        _connectionStatus.value = PrinterConnectionStatus.Connecting
        withContext(Dispatchers.IO) {
            try {
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket?.connect()
                outputStream = socket?.outputStream
                _connectionStatus.value = PrinterConnectionStatus.Connected(device.name ?: device.address)
            } catch (e: Exception) {
                _connectionStatus.value = PrinterConnectionStatus.Error(e.message ?: "Connection failed")
                socket = null
            }
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun print(zpl: String) {
        withContext(Dispatchers.IO) {
            try {
                outputStream?.write(zpl.toByteArray())
                outputStream?.flush()
            } catch (e: Exception) {
                _connectionStatus.value = PrinterConnectionStatus.Error("Print failed: ${e.message}")
                disconnect()
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (e: Exception) {
            // ignore
        }
        socket = null
        outputStream = null
        _connectionStatus.value = PrinterConnectionStatus.Disconnected
    }
}
