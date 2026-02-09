package com.example.warehouse.device

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class BluetoothPrinterManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val bluetoothManager = mockk<BluetoothManager>(relaxed = true)
    private val bluetoothAdapter = mockk<BluetoothAdapter>(relaxed = true)
    
    private lateinit var printerManager: BluetoothPrinterManager

    @Before
    fun setup() {
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothManager
        every { bluetoothManager.adapter } returns bluetoothAdapter
        
        printerManager = BluetoothPrinterManager(context)
    }

    @Test
    fun `startScan starts discovery if adapter enabled`() {
        // Given
        every { bluetoothAdapter.isEnabled } returns true

        // When
        printerManager.startScan()

        // Then
        verify { bluetoothAdapter.startDiscovery() }
    }

    @Test
    fun `startScan does not start discovery if adapter disabled`() {
        // Given
        every { bluetoothAdapter.isEnabled } returns false

        // When
        printerManager.startScan()

        // Then
        verify(exactly = 0) { bluetoothAdapter.startDiscovery() }
    }
    
    @Test
    fun `stopScan cancels discovery`() {
        // Given
        every { bluetoothAdapter.isDiscovering } returns true

        // When
        printerManager.stopScan()

        // Then
        verify { bluetoothAdapter.cancelDiscovery() }
    }
}
