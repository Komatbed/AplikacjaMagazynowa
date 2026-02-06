package com.example.warehouse.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.warehouse.data.local.SettingsDataStore
import com.example.warehouse.util.FileProcessingOptimizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.io.FileWriter

class FileOptimizationViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private var reservedWasteLengths: List<Int> = emptyList()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val _outputContent = MutableStateFlow("")
    val outputContent = _outputContent.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _selectedMode = MutableStateFlow(FileProcessingOptimizer.Mode.MIN_WASTE)
    val selectedMode = _selectedMode.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.reservedWasteLengths.collectLatest { csv ->
                reservedWasteLengths = csv.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .filter { it > 0 }
            }
        }
    }

    fun setMode(mode: FileProcessingOptimizer.Mode) {
        _selectedMode.value = mode
    }

    fun processFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _logs.value = listOf("Reading file: $uri")
            
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                } ?: ""

                if (content.isBlank()) {
                    _logs.value = _logs.value + "Error: File is empty or cannot be read."
                    _isProcessing.value = false
                    return@launch
                }

                val result = FileProcessingOptimizer.process(content, _selectedMode.value, reservedWasteLengths)
                
                _outputContent.value = result.outputLines.joinToString("\n")
                _logs.value = _logs.value + result.logs
                _logs.value = _logs.value + "Optimization complete. ${result.summary}"

            } catch (e: Exception) {
                _logs.value = _logs.value + "Error: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun processContent(content: String) {
        viewModelScope.launch {
             _isProcessing.value = true
            _logs.value = listOf("Processing pasted content...")
            
            try {
                val result = FileProcessingOptimizer.process(content, _selectedMode.value, reservedWasteLengths)
                
                _outputContent.value = result.outputLines.joinToString("\n")
                _logs.value = _logs.value + result.logs
                _logs.value = _logs.value + "Optimization complete. ${result.summary}"
            } catch (e: Exception) {
                 _logs.value = _logs.value + "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveOutput(context: Context, filename: String = "optimized.dcxtxt") {
        try {
            // Save to Downloads or Share?
            // For simplicity in this env, we can just log or try to share.
            // But let's assume we want to create a file to share.
            val file = File(context.cacheDir, filename)
            val writer = FileWriter(file)
            writer.write(_outputContent.value)
            writer.close()
            
            _logs.value = _logs.value + "Saved temporary file to: ${file.absolutePath}"
            // In UI, we can trigger Share intent.
        } catch (e: Exception) {
            _logs.value = _logs.value + "Error saving: ${e.message}"
        }
    }
}
